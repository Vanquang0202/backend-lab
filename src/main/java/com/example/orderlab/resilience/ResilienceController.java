package com.example.orderlab.resilience;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/resilience")
public class ResilienceController {
    private final ResilienceService service;

    public ResilienceController(ResilienceService service) { this.service = service; }

    @PostMapping("/payment")
    public org.springframework.http.ResponseEntity<PaymentDtos.PaymentResponse> payment(
            @Valid @RequestBody PaymentDtos.PaymentRequest request) {
        return service.pay(request);
    }

    @GetMapping("/circuit-breaker")
    public Map<String, Object> circuitBreaker() {
        return Map.of("name", "paymentCircuitBreaker", "state", service.state(),
                "failureRate", service.failureRate(), "bufferedCalls", service.bufferedCalls(),
                "failedCalls", service.failedCalls());
    }
}
