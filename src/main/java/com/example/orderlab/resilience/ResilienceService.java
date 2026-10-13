package com.example.orderlab.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Service
public class ResilienceService {
    private final RestClient paymentClient;
    private final String paymentUrl;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ResilienceService(RestClient paymentClient,
                             RetryRegistry retryRegistry,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             @Value("${app.resilience.payment-url:http://localhost:8080/fake-payment}") String paymentUrl) {
        this.paymentClient = paymentClient;
        this.paymentUrl = paymentUrl;
        this.retry = retryRegistry.retry("paymentRetry");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentCircuitBreaker");
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                System.out.printf("[RESILIENCE] circuit state=%s -> %s%n",
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
    }

    public ResponseEntity<PaymentDtos.PaymentResponse> pay(PaymentDtos.PaymentRequest request) {
        System.out.printf("[RESILIENCE] orderId=%s mode=%s circuit state=%s%n",
                request.orderId(), request.mode(), circuitBreaker.getState());
        AtomicInteger attempt = new AtomicInteger();
        Supplier<PaymentDtos.PaymentResponse> retryingCall = Retry.decorateSupplier(retry, () -> {
            int currentAttempt = attempt.incrementAndGet();
            System.out.printf("[RESILIENCE] orderId=%s attempt=%d%n", request.orderId(), currentAttempt);
            System.out.printf("[RESILIENCE] Calling payment... orderId=%s mode=%s%n", request.orderId(), request.mode());
            try {
                PaymentDtos.PaymentResponse response = paymentClient.post()
                        .uri(paymentUrl + "/payments")
                        .body(request)
                        .retrieve()
                        .body(PaymentDtos.PaymentResponse.class);
                System.out.printf("[RESILIENCE] orderId=%s downstream status=200%n", request.orderId());
                return response;
            } catch (HttpServerErrorException failure) {
                System.out.printf("[RESILIENCE] orderId=%s downstream status=%d%n",
                        request.orderId(), failure.getStatusCode().value());
                throw failure;
            } catch (ResourceAccessException failure) {
                System.out.printf("[RESILIENCE] orderId=%s timeout%n", request.orderId());
                throw new PaymentTimeoutException(failure);
            } catch (RestClientException failure) {
                if (isTimeout(failure)) {
                    System.out.printf("[RESILIENCE] orderId=%s timeout%n", request.orderId());
                    throw new PaymentTimeoutException(failure);
                }
                throw failure;
            }
        });

        // CircuitBreaker is the outer decorator: retries are bounded inside one breaker call.
        Supplier<PaymentDtos.PaymentResponse> protectedCall = CircuitBreaker.decorateSupplier(circuitBreaker, retryingCall);
        try {
            PaymentDtos.PaymentResponse response = protectedCall.get();
            return ResponseEntity.ok(response);
        } catch (CallNotPermittedException open) {
            System.out.printf("[RESILIENCE] orderId=%s circuit state=OPEN fail-fast%n", request.orderId());
            return fallback(request, "Payment service circuit is OPEN");
        } catch (Exception failure) {
            System.out.printf("[RESILIENCE] orderId=%s fallback error=%s%n", request.orderId(), failure.getClass().getSimpleName());
            return fallback(request, "Payment service temporarily unavailable");
        }
    }

    private ResponseEntity<PaymentDtos.PaymentResponse> fallback(PaymentDtos.PaymentRequest request, String message) {
        System.out.printf("[RESILIENCE] orderId=%s fallback%n", request.orderId());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new PaymentDtos.PaymentResponse("FALLBACK", message, request.orderId(), request.mode(), 0));
    }

    private boolean isTimeout(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
        }
        return failure.getMessage() != null && failure.getMessage().toLowerCase().contains("timeout");
    }

    public String state() { return circuitBreaker.getState().name(); }
    public float failureRate() { return circuitBreaker.getMetrics().getFailureRate(); }
    public int bufferedCalls() { return circuitBreaker.getMetrics().getNumberOfBufferedCalls(); }
    public int failedCalls() { return circuitBreaker.getMetrics().getNumberOfFailedCalls(); }
}
