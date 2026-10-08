package com.example.orderlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class OrderLabApplication {
    public static void main(String[] args) { SpringApplication.run(OrderLabApplication.class, args); }
}
