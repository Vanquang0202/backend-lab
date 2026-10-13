package com.example.orderlab.kafka;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent,Long> {
    boolean existsByEventId(String eventId);
    List<ProcessedEvent> findAllByOrderByProcessedAtDesc();
}
