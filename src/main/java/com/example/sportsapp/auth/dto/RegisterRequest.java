package com.example.sportsapp.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 12, message = "Password must be between 8 and 12 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,20}$",
            message = "Password must contain at least one digit, one lowercase letter, and one uppercase letter")
    private String password;

    @NotBlank
    @Size(min = 8, max = 12, message = "Password must be between 8 and 12 characters")
    private String confirmPassword;


    public String getConfirmPassword() {
        return confirmPassword;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
