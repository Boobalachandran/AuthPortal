package com.example.sportsapp.auth.controller;

import com.example.sportsapp.auth.dto.*;
import com.example.sportsapp.auth.service.AuthService;
import com.example.sportsapp.response.ChangePasswordResponse;
import com.example.sportsapp.response.LoginResponse;
import com.example.sportsapp.response.OtpResponse;
import com.example.sportsapp.response.SignupResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {this.authService = authService;}

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup (@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.signup(registerRequest));
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<OtpResponse> forgotPassword (@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        return ResponseEntity.ok(authService.generateOtp(forgotPasswordRequest));
    }

    @PostMapping("/verifyOtp")
    public ResponseEntity<String> verifyOtp( @Valid @RequestBody VerifyOtpRequest verifyOtpRequest){
        return ResponseEntity.ok(authService.verifyOtp(verifyOtpRequest));
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<ChangePasswordResponse> resetPassword (@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        return ResponseEntity.ok(authService.resetPassword(changePasswordRequest));
    }

}
