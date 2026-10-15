package com.epam.gymcrm.application.service;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException() {
        super("Invalid username or password");
    }
}
