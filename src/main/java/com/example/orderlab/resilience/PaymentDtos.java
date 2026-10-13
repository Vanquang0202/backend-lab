package com.example.orderlab.resilience;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record PaymentRequest(@NotBlank String orderId, @NotNull @Positive BigDecimal amount, @NotBlank String mode) {}
    public record PaymentResponse(String status, String message, String orderId, String mode, int downstreamAttempt) {}
}
