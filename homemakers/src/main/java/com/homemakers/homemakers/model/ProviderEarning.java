package com.homemakers.homemakers.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "provider_earnings")
public class ProviderEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================
    // RELATIONS
    // ========================
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Provider provider;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // ========================
    // EARNING DATA
    // ========================
    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EarningStatus status;

    private LocalDateTime createdAt;

    // ========================
    // LIFECYCLE
    // ========================
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ========================
    // GETTERS
    // ========================
    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public Booking getBooking() {
        return booking;
    }

    public double getAmount() {
        return amount;
    }

    public EarningStatus getStatus() {
        return status;
    }

    // ========================
    // SETTERS
    // ========================
    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setStatus(EarningStatus status) {
        this.status = status;
    }
}
