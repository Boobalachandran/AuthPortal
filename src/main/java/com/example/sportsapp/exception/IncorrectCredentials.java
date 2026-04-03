package com.example.sportsapp.exception;

public class IncorrectCredentials extends RuntimeException{
    public IncorrectCredentials(String message){
        super(message);
    }
}
