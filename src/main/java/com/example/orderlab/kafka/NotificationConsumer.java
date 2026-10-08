package com.example.orderlab.kafka;

import com.example.orderlab.order.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationConsumer {
    private final ProcessedEventRepository processed; private final boolean fail;
    public NotificationConsumer(ProcessedEventRepository processed,@Value("${app.notification.fail:false}") boolean fail){this.processed=processed;this.fail=fail;}
    @KafkaListener(topics="${app.kafka.topic}", groupId="${KAFKA_GROUP_ID:order-notification-group}")
    @Transactional
    public void consume(OrderEvent event) {
        if(processed.existsByEventId(event.eventId().toString())) { System.out.println("IDEMPOTENT SKIP event="+event.eventId()); return; }
        if(fail) throw new IllegalStateException("LAB_NOTIFICATION_FAIL=true");
        System.out.println("NOTIFICATION SENT order="+event.orderId()+" customer="+event.customerName());
        try { processed.saveAndFlush(new ProcessedEvent(event.eventId().toString())); }
        catch(DataIntegrityViolationException duplicate) { System.out.println("DUPLICATE SKIP event="+event.eventId()); }
    }
}
