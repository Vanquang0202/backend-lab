package com.example.orderlab.outbox;

import com.example.orderlab.order.Order;
import com.example.orderlab.order.OrderDtos;
import com.example.orderlab.order.OrderEvent;
import com.example.orderlab.order.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {
    private final OrderRepository orders;
    private final OutboxEventRepository events;
    private final ObjectMapper objectMapper;

    public OutboxService(OrderRepository orders, OutboxEventRepository events, ObjectMapper objectMapper) {
        this.orders = orders;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderDtos.Response create(OrderDtos.Request request) {
        Order order = orders.save(new Order(request.customerName(), request.totalAmount()));
        System.out.printf("[OUTBOX] order saved id=%s%n", order.getId());
        var orderEvent = new OrderEvent(UUID.randomUUID(), order.getId(), order.getCustomerName(),
                order.getTotalAmount(), Instant.now());
        try {
            events.save(new OutboxEvent("Order", order.getId(), "ORDER_CREATED",
                    objectMapper.writeValueAsString(orderEvent)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize ORDER_CREATED event", e);
        }
        System.out.printf("[OUTBOX] event stored aggregateId=%s eventType=ORDER_CREATED status=NEW%n", order.getId());
        return OrderDtos.Response.from(order);
    }

    @Transactional(readOnly = true)
    public java.util.List<OutboxDtos.EventResponse> findAll() {
        return events.findAllByOrderByCreatedAtAsc().stream().map(OutboxDtos.EventResponse::from).toList();
    }
}
