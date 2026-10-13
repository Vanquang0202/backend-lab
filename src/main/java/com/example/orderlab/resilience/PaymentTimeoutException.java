package com.example.orderlab.resilience;

public class PaymentTimeoutException extends RuntimeException {
    public PaymentTimeoutException(Throwable cause) { super("Payment downstream timed out", cause); }
}
