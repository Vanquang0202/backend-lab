package com.example.orderlab.rabbit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

@Component
public class RabbitJobConsumer {
    private final long processingDelayMs;

    public RabbitJobConsumer(@Value("${app.rabbit.processing-delay-ms:2000}") long processingDelayMs) {
        this.processingDelayMs = processingDelayMs;
    }

    @RabbitListener(queues = "${app.rabbit.queue}")
    public void consume(RabbitJobDtos.Job job) throws InterruptedException {
        int attempt = RetrySynchronizationManager.getContext() == null
                ? 1 : RetrySynchronizationManager.getContext().getRetryCount() + 1;
        System.out.printf("[RABBIT] Received eventId=%s attempt=%d%n", job.eventId(), attempt);
        System.out.printf("[RABBIT] jobType=%s%n", job.jobType());
        System.out.printf("[RABBIT] payload=%s%n", job.payload());
        if ("FAIL".equals(job.payload())) {
            System.out.printf("[RABBIT] Failure eventId=%s attempt=%d; retrying or sending to DLQ%n",
                    job.eventId(), attempt);
            throw new IllegalStateException("Intentional lab failure for payload=FAIL");
        }
        System.out.printf("[RABBIT] Processing eventId=%s%n", job.eventId());
        Thread.sleep(processingDelayMs);
        System.out.printf("[RABBIT] Completed eventId=%s%n", job.eventId());
    }
}
