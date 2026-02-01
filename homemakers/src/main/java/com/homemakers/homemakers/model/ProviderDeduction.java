package com.homemakers.homemakers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "provider_deductions")
public class ProviderDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Provider being penalized
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Provider provider;

    // WHY did this deduction happen
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionSourceType sourceType;

    // ID of the event (complaintId, bookingId, etc.)
    private Long sourceId;

    // WHAT kind of penalty
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionType type;

    // Monetary impact
    @Column(nullable = false)
    private double amount;

    // Lifecycle control
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionState state;

    // System vs admin
    @Column(nullable = false)
    private boolean systemGenerated;

    // Human explanation
    @Column(length = 255)
    private String reason;
    @ManyToOne(optional = false)
    private Booking booking;

    // Linked ONLY when payout is calculated
    @ManyToOne(fetch = FetchType.LAZY)
    private ProviderPayout payout;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        state = DeductionState.PROPOSED;
        systemGenerated = true;
    }

    /* ===== NO SETTERS FOR AMOUNT AFTER CREATION ===== */
    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public DeductionSourceType getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public DeductionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public DeductionState getState() {
        return state;
    }

    public boolean isSystemGenerated() {
        return systemGenerated;
    }

    public String getReason() {
        return reason;
    }

    public ProviderPayout getPayout() {
        return payout;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void setSourceType(DeductionSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public void setType(DeductionType type) {
        this.type = type;
    }

    /**
     * ⚠️ ONLY DeductionGenerator should call this
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setState(DeductionState state) {
        this.state = state;
    }

    public void setSystemGenerated(boolean systemGenerated) {
        this.systemGenerated = systemGenerated;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setPayout(ProviderPayout payout) {
        this.payout = payout;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }
}
