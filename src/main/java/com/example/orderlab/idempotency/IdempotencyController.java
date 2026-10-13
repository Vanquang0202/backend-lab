package com.example.orderlab.idempotency;

import com.example.orderlab.kafka.ProcessedEventRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/idempotency")
public class IdempotencyController {
    private final IdempotencyPublisher publisher;
    private final ProcessedEventRepository processed;

    public IdempotencyController(IdempotencyPublisher publisher, ProcessedEventRepository processed) {
        this.publisher = publisher;
        this.processed = processed;
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IdempotencyDtos.PublishResponse publish(@Valid @RequestBody IdempotencyDtos.PublishRequest request) {
        return publisher.publish(request);
    }

    @GetMapping("/processed-events")
    public List<IdempotencyDtos.ProcessedEventResponse> processedEvents() {
        return processed.findAllByOrderByProcessedAtDesc().stream()
                .map(event -> new IdempotencyDtos.ProcessedEventResponse(
                        event.getId(), event.getEventId(), event.getProcessedAt()))
                .toList();
    }
}
