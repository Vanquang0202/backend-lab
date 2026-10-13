package com.example.orderlab.idempotency;

import java.time.Instant;

public record IdempotencyEvent(String eventId, String orderId, String payload, Instant occurredAt) {}
