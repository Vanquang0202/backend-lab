package com.example.orderlab.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.core.KafkaOperations;

@org.springframework.context.annotation.Configuration
public class KafkaConfig {
    @Bean NewTopic orderCreatedTopic(@Value("${app.kafka.topic}") String topic){return new NewTopic(topic,3,(short)1);}
    @Bean DefaultErrorHandler errorHandler(KafkaTemplate<Object,Object> template,@Value("${app.notification.retry-backoff-ms:1000}") long delay){
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(delay,2));
    }
    @Bean ConcurrentKafkaListenerContainerFactory<String,Object> kafkaListenerContainerFactory(ConsumerFactory<String,Object> cf, DefaultErrorHandler eh){
        var f=new ConcurrentKafkaListenerContainerFactory<String,Object>(); f.setConsumerFactory(cf); f.setCommonErrorHandler(eh); return f;
    }
}
