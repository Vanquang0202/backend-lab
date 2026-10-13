package com.example.orderlab.outbox;

import com.example.orderlab.order.OrderDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/outbox")
public class OutboxController {
    private final OutboxService service;
    private final OutboxPublisher publisher;

    public OutboxController(OutboxService service, OutboxPublisher publisher) {
        this.service = service;
        this.publisher = publisher;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDtos.Response create(@Valid @RequestBody OrderDtos.Request request) {
        return service.create(request);
    }

    @GetMapping("/events")
    public List<OutboxDtos.EventResponse> events() { return service.findAll(); }

    @PostMapping("/failure-mode")
    public OutboxDtos.FailureModeResponse failureMode(@RequestBody OutboxDtos.FailureModeRequest request) {
        publisher.setFailureSimulation(request.enabled());
        return new OutboxDtos.FailureModeResponse(publisher.isFailureSimulationEnabled());
    }

    @GetMapping("/failure-mode")
    public OutboxDtos.FailureModeResponse failureMode() {
        return new OutboxDtos.FailureModeResponse(publisher.isFailureSimulationEnabled());
    }
}
