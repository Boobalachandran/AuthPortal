package com.example.sportsapp.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncorrectCredentials.class)
    public ResponseEntity<?> handleIncorrectCredentials(IncorrectCredentials ex) {
        return ResponseEntity
                .status(401)
                .body(Map.of("error", ex.getMessage()));
    }
}