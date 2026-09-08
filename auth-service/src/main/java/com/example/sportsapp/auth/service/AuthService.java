package com.example.sportsapp.auth.service;

import com.example.sportsapp.auth.dto.*;
import com.example.sportsapp.auth.entitiy.User;
import com.example.sportsapp.auth.repository.UserRepository;
import com.example.sportsapp.exception.IncorrectCredentials;
import com.example.sportsapp.kafka.event.UserSignedUpEvent;
import com.example.sportsapp.kafka.producer.KafkaProducerService;
import com.example.sportsapp.response.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Random;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaProducerService kafkaProducerService;
    private final Random random = new Random();

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public AuthService(UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder, OtpService otpService, EmailService emailService,
                       StringRedisTemplate stringRedisTemplate, KafkaProducerService kafkaProducerService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaProducerService = kafkaProducerService;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IncorrectCredentials("Invalid email or password"));


        String token = generateToken(user);
        return new LoginResponse(token, user.getEmail());
    }

    public SignupResponse signup(RegisterRequest registerRequest) {
        userRepository.findByEmail(registerRequest.getEmail()).ifPresent(u -> {
            throw new IncorrectCredentials("User with email " + registerRequest.getEmail() + " already exists");
        });

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new IncorrectCredentials("Password and confirm password do not match");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setProvider("LOCAL");
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userRepository.save(user);

        UserSignedUpEvent event =
                new UserSignedUpEvent(user.getId(), user.getEmail());

        kafkaProducerService.publishUserSignedUp(event);
        // Authenticate the newly registered user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getEmail(), registerRequest.getPassword())
        );

        return new SignupResponse(user.getId(), user.getEmail());
    }

    public ChangePasswordResponse resetPassword(ChangePasswordRequest changePasswordRequest) {
        User user  = userRepository.findByEmail(changePasswordRequest.getEmail())
                .orElseThrow(() -> new IncorrectCredentials("User with email " + changePasswordRequest.getEmail() + " not found"));

        if(changePasswordRequest.getToken() != null) {
            if (changePasswordRequest.getPassword().equals(changePasswordRequest.getConfirmPassword())) {
                if (passwordEncoder.matches(changePasswordRequest.getPassword(), user.getPassword())) {
                    throw new IncorrectCredentials("New password cannot be the same as the old password");
                }
            }else{
                throw new IncorrectCredentials("Password and Confirm password should be same");
            }
        }
        else{
            throw new IncorrectCredentials("Invalid token");
        }
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getPassword()));
        userRepository.save(user);

        return new ChangePasswordResponse(user.getId(),user.getEmail(),"Password changed successfully");
    }

    public OtpResponse generateOtp(ForgotPasswordRequest forgotPasswordRequest) {
        User user = userRepository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new IncorrectCredentials("User with email " + forgotPasswordRequest.getEmail() + " not found"));

        String otp = String.valueOf(random.nextInt(900000) + 100000);

        otpService.saveOtp(user.getEmail(), otp);
        emailService.sendOtp(user.getEmail(), otp);

        return new OtpResponse("OTP sent to " + user.getEmail());
    }

    public String verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        String email = verifyOtpRequest.getEmail();
        String redisKey = "otp:" + email;
        String storedOtp = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new IncorrectCredentials("OTP not found for email: " + email);
        }

        if (!storedOtp.equals(verifyOtpRequest.getOtp())) {
            throw new IncorrectCredentials("Invalid OTP");
        }

        stringRedisTemplate.delete(redisKey);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IncorrectCredentials("User not found for email: " + email));
        return generateToken(user);
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
