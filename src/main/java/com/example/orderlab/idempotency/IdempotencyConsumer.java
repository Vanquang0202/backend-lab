package com.example.orderlab.idempotency;

import com.example.orderlab.kafka.ProcessedEvent;
import com.example.orderlab.kafka.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyConsumer {
    private final ProcessedEventRepository processed;
    private final Set<String> failedBeforeMark = ConcurrentHashMap.newKeySet();

    public IdempotencyConsumer(ProcessedEventRepository processed) {
        this.processed = processed;
    }

    @KafkaListener(topics = "${app.kafka.idempotency-topic:idempotency-lab}",
            groupId = "${app.kafka.idempotency-group:idempotency-lab-group}")
    @Transactional
    public void consume(IdempotencyEvent event) {
        System.out.printf("[IDEMPOTENCY] Received eventId=%s%n", event.eventId());
        if (processed.existsByEventId(event.eventId())) {
            System.out.printf("[IDEMPOTENCY] Duplicate detected eventId=%s%n", event.eventId());
            System.out.printf("[IDEMPOTENCY] Skipping event eventId=%s%n", event.eventId());
            return;
        }

        System.out.printf("[IDEMPOTENCY] Not processed yet eventId=%s%n", event.eventId());
        System.out.printf("[IDEMPOTENCY] Processing eventId=%s orderId=%s payload=%s%n",
                event.eventId(), event.orderId(), event.payload());

        if ("FAIL_BEFORE_MARK".equals(event.payload()) && failedBeforeMark.add(event.eventId())) {
            System.out.printf("[IDEMPOTENCY] Failure before mark eventId=%s; Kafka will redeliver%n", event.eventId());
            throw new IllegalStateException("Intentional failure before processed_events insert");
        }

        try {
            processed.saveAndFlush(new ProcessedEvent(event.eventId()));
            System.out.printf("[IDEMPOTENCY] Saved processed event eventId=%s%n", event.eventId());
        } catch (DataIntegrityViolationException duplicate) {
            System.out.printf("[IDEMPOTENCY] Duplicate detected at insert eventId=%s%n", event.eventId());
            System.out.printf("[IDEMPOTENCY] Skipping event eventId=%s%n", event.eventId());
            return;
        }
        System.out.printf("[IDEMPOTENCY] Completed eventId=%s%n", event.eventId());
    }
}
