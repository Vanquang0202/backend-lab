package com.example.orderlab.order;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {}
    public record Request(@NotBlank String customerName, @NotNull @Positive BigDecimal totalAmount, String status) {}
    public record Response(UUID id, String customerName, BigDecimal totalAmount, String status, Instant createdAt, Instant updatedAt) implements java.io.Serializable {
        public static Response from(Order o) { return new Response(o.getId(),o.getCustomerName(),o.getTotalAmount(),o.getStatus(),o.getCreatedAt(),o.getUpdatedAt()); }
    }
}
