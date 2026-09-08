package com.example.sportsapp.kafka.producer;

import com.example.sportsapp.kafka.event.UserSignedUpEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "user-signed-up";

    private final KafkaTemplate<String, UserSignedUpEvent> kafkaTemplate;

    public KafkaProducerService(
            KafkaTemplate<String, UserSignedUpEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserSignedUp(UserSignedUpEvent event) {
        kafkaTemplate.send(TOPIC, event.getEmail(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("Kafka publish failed: " + ex.getMessage());
                    } else {
                        System.out.println(
                                "Kafka publish successful: topic="
                                        + result.getRecordMetadata().topic()
                                        + ", partition="
                                        + result.getRecordMetadata().partition()
                                        + ", offset="
                                        + result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}