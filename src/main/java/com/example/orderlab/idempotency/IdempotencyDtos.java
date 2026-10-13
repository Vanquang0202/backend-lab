package com.example.orderlab.idempotency;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class IdempotencyDtos {
    private IdempotencyDtos() {}

    public record PublishRequest(@NotBlank String eventId, @NotBlank String orderId, @NotBlank String payload) {}
    public record PublishResponse(String eventId, String orderId, String payload, String topic, String status) {}
    public record ProcessedEventResponse(Long id, String eventId, Instant processedAt) {}
}
