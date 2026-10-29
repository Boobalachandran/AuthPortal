package com.example.sportsapp;

import com.example.sportsapp.kafka.event.UserSignedUpEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for AuthPortal integration tests.
 *
 * This configuration provides mock implementations of external service dependencies
 * (Kafka, Redis, Mail) to allow tests to run without requiring those services to be
 * running locally. This keeps tests fast and independent of infrastructure.
 *
 * The mocks allow the Spring context to load successfully during @SpringBootTest,
 * while actual service behavior can be mocked individually in specific tests.
 *
 * This configuration is only used when the "test" Spring profile is active.
 */
@TestConfiguration
public class TestAuthPortalConfiguration {

    /**
     * Mock KafkaTemplate for publishing UserSignedUpEvent events.
     * Allows KafkaProducerService to be instantiated without requiring Kafka broker connection.
     */
    @Bean
    @Primary
    public KafkaTemplate<String, UserSignedUpEvent> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    /**
     * Mock StringRedisTemplate for OTP storage and retrieval.
     * Allows OtpService to be instantiated without requiring Redis connection.
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    /**
     * Mock JavaMailSender for sending OTP emails.
     * Allows EmailService to be instantiated without requiring SMTP server connection.
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}




