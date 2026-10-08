package com.example.orderlab.order;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository repo; private final KafkaTemplate<String, OrderEvent> kafka;
    private final String topic;
    public OrderService(OrderRepository repo, KafkaTemplate<String, OrderEvent> kafka,
                        @org.springframework.beans.factory.annotation.Value("${app.kafka.topic}") String topic) { this.repo=repo; this.kafka=kafka; this.topic=topic; }
    @Transactional
    public OrderDtos.Response create(OrderDtos.Request r) {
        Order saved=repo.save(new Order(r.customerName(),r.totalAmount()));
        kafka.send(topic, saved.getId().toString(), new OrderEvent(UUID.randomUUID(), saved.getId(), saved.getCustomerName(), saved.getTotalAmount(), Instant.now()));
        return OrderDtos.Response.from(saved);
    }
    @Cacheable(value="orders", key="#id")
    @Transactional(readOnly=true)
    public OrderDtos.Response get(UUID id) { return repo.findById(id).map(OrderDtos.Response::from).orElseThrow(() -> new OrderNotFoundException(id)); }
    @Transactional
    @CacheEvict(value="orders", key="#id")
    public OrderDtos.Response update(UUID id, OrderDtos.Request r) { Order o=repo.findById(id).orElseThrow(() -> new OrderNotFoundException(id)); o.update(r.customerName(),r.totalAmount(),r.status()==null?o.getStatus():r.status()); return OrderDtos.Response.from(repo.save(o)); }
    @Transactional
    @CacheEvict(value="orders", key="#id")
    public void delete(UUID id) { if (!repo.existsById(id)) throw new OrderNotFoundException(id); repo.deleteById(id); }
}
