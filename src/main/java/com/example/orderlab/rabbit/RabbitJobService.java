package com.example.orderlab.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RabbitJobService {
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitJobService(RabbitTemplate rabbitTemplate,
                            @Value("${app.rabbit.exchange}") String exchange,
                            @Value("${app.rabbit.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public RabbitJobDtos.Response publish(RabbitJobDtos.Request request) {
        var job = new RabbitJobDtos.Job(UUID.randomUUID(), request.jobType(), request.payload());
        rabbitTemplate.convertAndSend(exchange, routingKey, job);
        return new RabbitJobDtos.Response(job.eventId(), job.jobType(), job.payload(),
                exchange, routingKey, "PUBLISHED");
    }
}
