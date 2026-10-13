package com.example.orderlab.outbox;

import java.time.Instant;
import java.util.UUID;

public final class OutboxDtos {
    private OutboxDtos() {}

    public record EventResponse(UUID id, String aggregateType, UUID aggregateId, String eventType,
                                String payload, String status, Instant createdAt, Instant publishedAt,
                                int retryCount, String lastError) {
        static EventResponse from(OutboxEvent event) {
            return new EventResponse(event.getId(), event.getAggregateType(), event.getAggregateId(),
                    event.getEventType(), event.getPayload(), event.getStatus(), event.getCreatedAt(),
                    event.getPublishedAt(), event.getRetryCount(), event.getLastError());
        }
    }

    public record FailureModeRequest(boolean enabled) {}
    public record FailureModeResponse(boolean enabled) {}
}
