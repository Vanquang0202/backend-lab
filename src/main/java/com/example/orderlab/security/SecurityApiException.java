package com.example.orderlab.security;

import org.springframework.http.HttpStatus;

public class SecurityApiException extends RuntimeException {
    private final HttpStatus status;
    private final String error;

    public SecurityApiException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() { return status; }
    public String getError() { return error; }
}
