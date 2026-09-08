package com.example.sportsapp.kafka.event;

public class UserSignedUpEvent {

    private Long userId;
    private String email;

    public UserSignedUpEvent() {
    }

    public UserSignedUpEvent(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}