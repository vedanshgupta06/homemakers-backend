package com.homemakers.homemakers.model;

import com.homemakers.homemakers.model.PaymentMethod;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long bookingId; // nullable for wallet recharge

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PAID, FAILED

    @Enumerated(EnumType.STRING)
    private PaymentMethod method; // WALLET, STRIPE

    private String stripePaymentIntent;

    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getStripePaymentIntent() {
        return stripePaymentIntent;
    }

    public void setStripePaymentIntent(String stripePaymentIntent) {
        this.stripePaymentIntent = stripePaymentIntent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // getters & setters
}
