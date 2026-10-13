package com.example.orderlab.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OutboxStateService {
    private final OutboxEventRepository events;

    public OutboxStateService(OutboxEventRepository events) {
        this.events = events;
    }

    @Transactional
    public OutboxEvent claimNext(int maxRetry) {
        OutboxEvent event = events.findFirstByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                List.of("NEW", "FAILED"), maxRetry);
        if (event == null) return null;
        event.markProcessing();
        return events.saveAndFlush(event);
    }

    @Transactional
    public void markPublished(UUID id) {
        events.findById(id).ifPresent(event -> {
            event.markPublished();
            events.save(event);
        });
    }

    @Transactional
    public void markFailed(UUID id, String error) {
        events.findById(id).ifPresent(event -> {
            event.markFailed(error);
            events.save(event);
        });
    }
}
