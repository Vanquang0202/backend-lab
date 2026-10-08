package com.example.orderlab.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String customerName;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal totalAmount;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Order() {}
    public Order(String customerName, BigDecimal totalAmount) {
        this.customerName = customerName; this.totalAmount = totalAmount; this.status = "NEW";
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public UUID getId(){return id;} public String getCustomerName(){return customerName;}
    public BigDecimal getTotalAmount(){return totalAmount;} public String getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void update(String customerName, BigDecimal totalAmount, String status) {
        this.customerName=customerName; this.totalAmount=totalAmount; this.status=status;
    }
}
