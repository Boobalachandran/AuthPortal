package com.example.notification.kafka;

import com.example.notification.service.EmailService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserSignedUpConsumer {

    private final EmailService emailService;
    private final Set<String> processedSignupKeys = ConcurrentHashMap.newKeySet();

    public UserSignedUpConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "user-signed-up", groupId = "notification-service")
    public void consume(UserSignedUpEvent event) {
        String idempotencyKey = event.getUserId() != null
                ? "userId:" + event.getUserId()
                : "email:" + event.getEmail();

        if (!processedSignupKeys.add(idempotencyKey)) {
            System.out.println("Duplicate signup event ignored for key=" + idempotencyKey);
            return;
        }

        System.out.println("UserSignedUp event received: userId=" + event.getUserId() + ", email=" + event.getEmail());
        emailService.sendWelcomeEmail(event.getEmail());
    }
}

