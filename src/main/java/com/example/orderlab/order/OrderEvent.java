package com.example.orderlab.order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record OrderEvent(UUID eventId, UUID orderId, String customerName, BigDecimal totalAmount, Instant occurredAt) {}
