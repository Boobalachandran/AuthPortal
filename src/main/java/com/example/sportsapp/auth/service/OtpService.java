package com.example.sportsapp.auth.service;

import com.example.sportsapp.auth.entitiy.Otp;
import com.example.sportsapp.auth.repository.OtpRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpRepository otpRepository;

    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    public void saveOtp(String email, String otp) {

        Otp otpEntity = new Otp();
        if(otpRepository.findByEmail(email).isPresent()){
            otpEntity = otpRepository.findByEmail(email).get();
        }else {
            otpEntity.setEmail(email);
        }
        otpEntity.setOtp(otp);
        otpEntity.setStatus(0);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(otpEntity);
    }
}
