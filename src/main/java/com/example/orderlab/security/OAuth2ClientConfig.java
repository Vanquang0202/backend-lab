package com.example.orderlab.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import static org.springframework.security.config.oauth2.client.CommonOAuth2Provider.GOOGLE;

@Configuration
@ConditionalOnProperty(name = "app.security.oauth2.google.enabled", havingValue = "true")
public class OAuth2ClientConfig {
    @Bean
    ClientRegistrationRepository googleClientRegistrationRepository(
            @org.springframework.beans.factory.annotation.Value("${GOOGLE_CLIENT_ID}") String clientId,
            @org.springframework.beans.factory.annotation.Value("${GOOGLE_CLIENT_SECRET}") String clientSecret) {
        ClientRegistration registration = GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .userNameAttributeName("sub")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
