package com.example.orderlab.resilience;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ResilienceHttpConfig {
    @Bean
    RestClient paymentRestClient(RestTemplateBuilder builder,
                                 @Value("${app.resilience.payment-timeout.connect-ms:2000}") int connectTimeoutMs,
                                 @Value("${app.resilience.payment-timeout.read-ms:3000}") int readTimeoutMs) {
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
