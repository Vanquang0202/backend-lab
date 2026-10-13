package com.example.orderlab.outbox;

import com.example.orderlab.order.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class OutboxPublisher {
    private final OutboxStateService state;
    private final KafkaTemplate<String, OrderEvent> kafka;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final int maxRetry;
    private final long sendTimeoutSeconds;
    private final AtomicBoolean simulateFailure;

    public OutboxPublisher(OutboxStateService state, KafkaTemplate<String, OrderEvent> kafka,
                           ObjectMapper objectMapper,
                           @Value("${app.kafka.topic}") String topic,
                           @Value("${app.outbox.max-retry:5}") int maxRetry,
                           @Value("${app.outbox.send-timeout-seconds:10}") long sendTimeoutSeconds,
                           @Value("${app.outbox.simulate-kafka-failure:false}") boolean simulateFailure) {
        this.state = state;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.maxRetry = maxRetry;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
        this.simulateFailure = new AtomicBoolean(simulateFailure);
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:2000}")
    public void publishNext() {
        OutboxEvent event = state.claimNext(maxRetry);
        if (event == null) return;
        try {
            System.out.printf("[OUTBOX] publishing event id=%s type=%s attempt=%d%n",
                    event.getId(), event.getEventType(), event.getRetryCount() + 1);
            if (simulateFailure.get()) throw new IllegalStateException("OUTBOX_SIMULATE_KAFKA_FAILURE=true");
            OrderEvent payload = objectMapper.readValue(event.getPayload(), OrderEvent.class);
            kafka.send(topic, event.getAggregateId().toString(), payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            state.markPublished(event.getId());
            System.out.printf("[OUTBOX] publish success id=%s topic=%s%n", event.getId(), topic);
        } catch (Exception failure) {
            String message = failure.getCause() == null ? failure.getMessage() : failure.getCause().toString();
            state.markFailed(event.getId(), message == null ? failure.getClass().getSimpleName() : message);
            System.out.printf("[OUTBOX] publish failed id=%s error=%s retryCount=%d/%d%n",
                    event.getId(), message, event.getRetryCount() + 1, maxRetry);
        }
    }

    public boolean isFailureSimulationEnabled() { return simulateFailure.get(); }
    public void setFailureSimulation(boolean enabled) { simulateFailure.set(enabled); }
}
