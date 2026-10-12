package com.example.orderlab.rabbit;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class RabbitJobDtos {
    private RabbitJobDtos() {}

    public record Request(@NotBlank String jobType, @NotBlank String payload) {}

    public record Job(UUID eventId, String jobType, String payload) {}

    public record Response(UUID eventId, String jobType, String payload,
                           String exchange, String routingKey, String status) {}
}
