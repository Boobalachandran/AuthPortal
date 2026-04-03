package com.example.sportsapp.auth.controller;

import com.example.sportsapp.auth.dto.LoginRequest;
import com.example.sportsapp.auth.dto.RegisterRequest;
import com.example.sportsapp.auth.entitiy.User;
import com.example.sportsapp.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;

    public AuthController(AuthService authService) {this.authService = authService;}

    @PostMapping("/login")
    public ResponseEntity<?> login (@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup (@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.signup(registerRequest));
    }
}
