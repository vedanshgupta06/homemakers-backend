package com.homemakers.homemakers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "provider_payouts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider_id", "payout_month"})
        }
)
public class ProviderPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    // Keep for now (monthly), can evolve later
    @Column(name = "payout_month", nullable = false)
    private String payoutMonth;

    // SNAPSHOT VALUES (IMMUTABLE AFTER CALCULATION)
    @Column(nullable = false)
    private double totalEarnings;

    @Column(nullable = false)
    private double totalDeductions;

    @Column(nullable = false)
    private double netPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status; // INITIATED, CALCULATED, PAID

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = PayoutStatus.CALCULATED;
    }

    // =======================
    // GETTERS
    // =======================

    public Long getId() { return id; }
    public Provider getProvider() { return provider; }
    public String getPayoutMonth() { return payoutMonth; }
    public double getTotalEarnings() { return totalEarnings; }
    public double getTotalDeductions() { return totalDeductions; }
    public double getNetPayout() { return netPayout; }
    public PayoutStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }

    // =======================
    // SETTERS (SERVICE ONLY)
    // =======================

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void setPayoutMonth(String payoutMonth) {
        this.payoutMonth = payoutMonth;
    }

    public void setTotalEarnings(double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public void setTotalDeductions(double totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public void setNetPayout(double netPayout) {
        this.netPayout = netPayout;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public void markPaid() {
        this.status = PayoutStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void setId(Long id) {
        this.id = id;
    }
}
