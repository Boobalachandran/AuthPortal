package com.example.sportsapp.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private final StringRedisTemplate stringRedisTemplate;

    public OtpService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void saveOtp(String email, String otp) {
        String redisKey = "otp:" + email;
        stringRedisTemplate.opsForValue().set(redisKey, otp, 5, TimeUnit.MINUTES);
    }
}
