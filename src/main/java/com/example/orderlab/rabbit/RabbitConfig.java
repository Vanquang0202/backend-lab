package com.example.orderlab.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitConfig {
    @Bean
    MessageConverter rabbitMessageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  MessageConverter rabbitMessageConverter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        return template;
    }

    @Bean
    DirectExchange jobsExchange(@Value("${app.rabbit.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    DirectExchange jobsDeadLetterExchange(@Value("${app.rabbit.dlx}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    Queue jobsQueue(@Value("${app.rabbit.queue}") String queue,
                    @Value("${app.rabbit.dlx}") String dlx,
                    @Value("${app.rabbit.dlq}") String dlq) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(dlx)
                .deadLetterRoutingKey(dlq)
                .build();
    }

    @Bean
    Queue jobsDlq(@Value("${app.rabbit.dlq}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    Binding jobsBinding(@Qualifier("jobsQueue") Queue jobsQueue,
                       @Qualifier("jobsExchange") DirectExchange jobsExchange,
                       @Value("${app.rabbit.routing-key}") String routingKey) {
        return BindingBuilder.bind(jobsQueue).to(jobsExchange).with(routingKey);
    }

    @Bean
    Binding jobsDlqBinding(@Qualifier("jobsDlq") Queue jobsDlq,
                          @Qualifier("jobsDeadLetterExchange") DirectExchange jobsDeadLetterExchange,
                          @Value("${app.rabbit.dlq}") String routingKey) {
        return BindingBuilder.bind(jobsDlq).to(jobsDeadLetterExchange).with(routingKey);
    }

    @Bean
    MethodInterceptor rabbitRetryInterceptor(
            @Value("${app.rabbit.retry-backoff-ms:1000}") long backoffMs) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(backoffMs, 1.0, backoffMs)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter,
            MethodInterceptor rabbitRetryInterceptor) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(rabbitRetryInterceptor);
        factory.setErrorHandler(new ConditionalRejectingErrorHandler());
        return factory;
    }
}
