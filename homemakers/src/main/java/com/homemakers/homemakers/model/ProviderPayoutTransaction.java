package com.homemakers.homemakers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "provider_payout_transactions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "provider_id",
                        "booking_id",
                        "payout_month",
                        "week_no"
                }
        )
)
public class ProviderPayoutTransaction {

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
    // PAYOUT DATA
    // ========================
    @Column(nullable = false)
    private String payoutMonth; // yyyy-MM

    @Column(nullable = false)
    private int weekNo; // 1–4 per booking

    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeeklyPayoutStatus status = WeeklyPayoutStatus.AVAILABLE;


    private LocalDateTime createdAt;
    private LocalDateTime requestedAt;

    private LocalDateTime paidAt;

    @Column(name = "reference_id")
    private String referenceId;

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

    public String getPayoutMonth() {
        return payoutMonth;
    }

    public int getWeekNo() {
        return weekNo;
    }

    public double getAmount() {
        return amount;
    }



    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getReferenceId() {
        return referenceId;
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

    public void setPayoutMonth(String payoutMonth) {
        this.payoutMonth = payoutMonth;
    }

    public void setWeekNo(int weekNo) {
        this.weekNo = weekNo;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }



    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public WeeklyPayoutStatus getStatus() {
        return status;
    }

    public void setStatus(WeeklyPayoutStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
