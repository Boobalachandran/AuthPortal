package com.example.sportsapp.auth.service;

import com.example.sportsapp.auth.dto.LoginRequest;
import com.example.sportsapp.auth.dto.RegisterRequest;
import com.example.sportsapp.auth.entitiy.User;
import com.example.sportsapp.auth.repository.UserRepository;
import com.example.sportsapp.exception.IncorrectCredentials;
import com.example.sportsapp.response.LoginResponse;
import com.example.sportsapp.response.SignupResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;


@Service
public class AuthService {
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IncorrectCredentials("Invalid email or password"));

        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new IncorrectCredentials("Invalid email or password");
        }

        String token = generateToken(user);
        return new LoginResponse(token, user.getEmail());
    }

    public SignupResponse signup(RegisterRequest registerRequest) {
        userRepository.findByEmail(registerRequest.getEmail()).ifPresent(u -> {
            throw new IncorrectCredentials("User with email " + registerRequest.getEmail() + " already exists");
        });

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        if(registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            user.setPassword(registerRequest.getPassword());
        } else {
            throw new IncorrectCredentials("Password and confirm password do not match");
        }
        userRepository.save(user);

        return new SignupResponse(user.getId(), user.getEmail());
    }

    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }
}
