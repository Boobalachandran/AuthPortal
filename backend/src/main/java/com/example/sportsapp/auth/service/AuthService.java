package com.example.sportsapp.auth.service;

import com.example.sportsapp.auth.dto.*;
import com.example.sportsapp.auth.entitiy.Otp;
import com.example.sportsapp.auth.entitiy.User;
import com.example.sportsapp.auth.repository.OtpRepository;
import com.example.sportsapp.auth.repository.UserRepository;
import com.example.sportsapp.exception.IncorrectCredentials;
import com.example.sportsapp.response.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Random;

import static java.time.ZoneId.systemDefault;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final JavaMailSenderImpl mailSender;
    private final Random random = new Random();


    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public AuthService(UserRepository userRepository, OtpRepository otpRepository, JavaMailSenderImpl mailSender) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.mailSender = mailSender;
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
        user.setProvider("LOCAL");
        if(registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            user.setPassword(registerRequest.getPassword());
        } else {
            throw new IncorrectCredentials("Password and confirm password do not match");
        }
        userRepository.save(user);

        return new SignupResponse(user.getId(), user.getEmail());
    }

    public ChangePasswordResponse resetPassword(ChangePasswordRequest changePasswordRequest) {
        User user  = userRepository.findByEmail(changePasswordRequest.getEmail())
                .orElseThrow(() -> new IncorrectCredentials("User with email " + changePasswordRequest.getEmail() + " not found"));

        if(changePasswordRequest.getToken() != null) {
            if (changePasswordRequest.getPassword().equals(changePasswordRequest.getConfirmPassword())) {
                if (user.getPassword().equals(changePasswordRequest.getPassword())) {
                    throw new IncorrectCredentials("New password cannot be the same as the old password");
                }
            }else{
                throw new IncorrectCredentials("Password and Confirm password should be same");
            }
        }
        else{
            throw new IncorrectCredentials("Invalid token");

        }
        user.setPassword(changePasswordRequest.getPassword());
        userRepository.save(user);

        return new ChangePasswordResponse(user.getId(),user.getEmail(),"Password changed successfully");
    }

    public OtpResponse generateOtp(ForgotPasswordRequest forgotPasswordRequest) {
        User user = userRepository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new IncorrectCredentials("User with email " + forgotPasswordRequest.getEmail() + " not found"));

        String otp = String.valueOf(random.nextInt(900000) + 100000);

        OtpService otpService = new OtpService(otpRepository);
        EmailService emailService = new EmailService(mailSender);
        otpService.saveOtp(user.getEmail(), otp);
        emailService.sendOtp(user.getEmail(), otp);

        return new OtpResponse("OTP sent to " + user.getEmail());
    }

    public String verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        Otp otp = otpRepository.findByEmail(verifyOtpRequest.getEmail())
                .orElseThrow(() -> new IncorrectCredentials("OTP not found for email: " + verifyOtpRequest.getEmail()));
        
        if(System.currentTimeMillis() > otp.getExpiryTime().atZone(systemDefault()).toInstant().toEpochMilli() ||
                (!otp.getOtp().equals(verifyOtpRequest.getOtp())) ) {
            throw new IncorrectCredentials("Invalid OTP");
        } else if (otp.getStatus() == 1) {
            throw new IncorrectCredentials("OTP has already been used");
        } else {
            otp.setStatus(1);
            otpRepository.save(otp);
            User user = userRepository.findByEmail(verifyOtpRequest.getEmail())
                    .orElseThrow(() -> new IncorrectCredentials("User not found for email: " + verifyOtpRequest.getEmail()));
            return generateToken(user);
        }
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
