package com.example.orderlab.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SecurityExceptionHandler {
    @ExceptionHandler(SecurityApiException.class)
    public ResponseEntity<Map<String, Object>> handle(SecurityApiException error) {
        return ResponseEntity.status(error.getStatus()).body(Map.of(
                "status", error.getStatus().value(), "error", error.getError(), "message", error.getMessage()));
    }
}
