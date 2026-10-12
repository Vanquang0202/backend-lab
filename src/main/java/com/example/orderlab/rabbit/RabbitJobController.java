package com.example.orderlab.rabbit;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rabbit/jobs")
public class RabbitJobController {
    private final RabbitJobService service;

    public RabbitJobController(RabbitJobService service) {
        this.service = service;
    }

    @PostMapping
    public RabbitJobDtos.Response publish(@Valid @RequestBody RabbitJobDtos.Request request) {
        return service.publish(request);
    }
}
