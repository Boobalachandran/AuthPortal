package com.example.sportsapp.auth.repository;

import com.example.sportsapp.auth.entitiy.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByEmail(String email);
}
