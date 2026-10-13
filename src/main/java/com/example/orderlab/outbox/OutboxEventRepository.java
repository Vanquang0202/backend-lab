package com.example.orderlab.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findAllByOrderByCreatedAtAsc();
    OutboxEvent findFirstByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(List<String> statuses, int maxRetry);
}
