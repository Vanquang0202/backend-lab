package com.example.orderlab.idempotency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IdempotencyPublisher {
    private final KafkaTemplate<String, IdempotencyEvent> kafka;
    private final String topic;

    public IdempotencyPublisher(KafkaTemplate<String, IdempotencyEvent> kafka,
                                @Value("${app.kafka.idempotency-topic:idempotency-lab}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public IdempotencyDtos.PublishResponse publish(IdempotencyDtos.PublishRequest request) {
        var event = new IdempotencyEvent(request.eventId(), request.orderId(), request.payload(), Instant.now());
        kafka.send(topic, event.eventId(), event);
        return new IdempotencyDtos.PublishResponse(event.eventId(), event.orderId(), event.payload(), topic, "PUBLISHED");
    }
}
