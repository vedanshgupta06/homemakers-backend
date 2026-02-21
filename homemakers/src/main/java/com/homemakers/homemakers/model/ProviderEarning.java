package com.homemakers.homemakers.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "provider_earnings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"provider_id", "booking_id", "week_no"}
        )
)

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
    // EARNING DATA of providers
    // ========================
    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EarningStatus status;

    private LocalDateTime createdAt;
    @Version
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id")
    private ProviderPayout payout;


    // ========================
    // LIFECYCLE
    // ========================
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
    private int weekNo;


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

    public int getWeekNo() {
        return weekNo;
    }

    public void setWeekNo(int weekNo) {
        this.weekNo = weekNo;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public ProviderPayout getPayout() {
        return payout;
    }

    public void setPayout(ProviderPayout payout) {
        this.payout = payout;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
