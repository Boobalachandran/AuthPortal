package com.example.sportsapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=0",  // Disable Redis connection pool to port 0
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                               "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@ActiveProfiles("test")
@Import(TestAuthPortalConfiguration.class)
class AuthPortalApplicationTests {

    @Test
    void contextLoads() {
    }

}
