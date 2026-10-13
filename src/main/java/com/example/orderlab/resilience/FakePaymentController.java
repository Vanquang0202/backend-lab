package com.example.orderlab.resilience;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/fake-payment")
public class FakePaymentController {
    private final ConcurrentHashMap<String, AtomicInteger> flakyAttempts = new ConcurrentHashMap<>();

    @PostMapping("/payments")
    public ResponseEntity<PaymentDtos.PaymentResponse> pay(@Valid @RequestBody PaymentDtos.PaymentRequest request)
            throws InterruptedException {
        String mode = request.mode().toUpperCase(Locale.ROOT);
        int attempt = "FLAKY".equals(mode)
                ? flakyAttempts.computeIfAbsent(request.orderId(), ignored -> new AtomicInteger()).incrementAndGet()
                : 1;
        System.out.printf("[FAKE-PAYMENT] orderId=%s mode=%s attempt=%d%n", request.orderId(), mode, attempt);

        return switch (mode) {
            case "NORMAL" -> ok(request, attempt);
            case "SLOW" -> slow(request, attempt);
            case "ERROR" -> error(request, attempt);
            case "FLAKY" -> attempt <= 2 ? error(request, attempt) : ok(request, attempt);
            default -> ResponseEntity.badRequest().body(
                    new PaymentDtos.PaymentResponse("ERROR", "Unsupported payment mode", request.orderId(), mode, attempt));
        };
    }

    @PostMapping("/reset")
    public void reset() { flakyAttempts.clear(); }

    private ResponseEntity<PaymentDtos.PaymentResponse> ok(PaymentDtos.PaymentRequest request, int attempt) {
        return ResponseEntity.ok(new PaymentDtos.PaymentResponse("APPROVED", "Payment approved", request.orderId(), request.mode(), attempt));
    }

    private ResponseEntity<PaymentDtos.PaymentResponse> error(PaymentDtos.PaymentRequest request, int attempt) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new PaymentDtos.PaymentResponse("ERROR", "Fake payment failure", request.orderId(), request.mode(), attempt));
    }

    private ResponseEntity<PaymentDtos.PaymentResponse> slow(PaymentDtos.PaymentRequest request, int attempt)
            throws InterruptedException {
        Thread.sleep(9000);
        return ok(request, attempt);
    }
}
