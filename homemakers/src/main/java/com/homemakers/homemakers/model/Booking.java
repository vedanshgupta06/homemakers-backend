//package com.homemakers.homemakers.model;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import jakarta.persistence.*;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.Set;
//
//@Entity
//@Table(name = "bookings")
//public class Booking {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(name = "booking_services", joinColumns = @JoinColumn(name = "booking_id"))
//    @Enumerated(EnumType.STRING)
//    @Column(name = "service")
//    private Set<ServiceType> services;
//    private Double walletUsed = 0.0;
//    private Double finalPayableAmount;
//    @Column(nullable = false)
//    private double totalPrice;
//
//
//    private Integer hoursPerDay;
//    @ManyToOne
//    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//
//    private User user;
//
//    @ManyToOne
//    @JsonIgnoreProperties({"availabilities", "hibernateLazyInitializer", "handler"})
//    private Provider provider;
//
//    @OneToOne(optional = false)
//    @JoinColumn(name = "availability_id", unique = true)
//    private ProviderAvailability availability;
//
//    @Enumerated(EnumType.STRING)
//    private BookingStatus status = BookingStatus.PENDING;
//
//    private int totalDays;
//    private int holidays;
//    private int chargeableDays;
//
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//    private LocalDateTime completedAt;
//
//    // 🔥 Proper service period tracking
//    private LocalDate workStartDate;
//    private LocalDate workEndDate;
//
//    @PrePersist
//    void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = createdAt;
//    }
//
//    @PreUpdate
//    void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
//
//    private String stripeSessionId;
//
//    private String stripePaymentIntent;
//
//    @Column(name = "booking_start_time")
//    private LocalTime bookingStartTime;
//
//    @Column(name = "booking_end_time")
//    private LocalTime bookingEndTime;
//
//    private boolean settlementDone = false;
//    // Add this field
//    @Column(nullable = true)
//    private Double penaltyApplied;
//
//    // Add getter and setter
//    public Double getPenaltyApplied() { return penaltyApplied; }
//    public void setPenaltyApplied(Double penaltyApplied) {
//        this.penaltyApplied = penaltyApplied;
//    }
//    public boolean isSettlementDone() {
//        return settlementDone;
//    }
//
//    public void setSettlementDone(boolean settlementDone) {
//        this.settlementDone = settlementDone;
//    }
//    // ================= GETTERS & SETTERS =================
//
//    public Long getId() { return id; }
//
//    public Set<ServiceType> getServices() { return services; }
//
//    public void setServices(Set<ServiceType> services) { this.services = services; }
//
//    public double getTotalPrice() { return totalPrice; }
//
//    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
//
//    public Integer getHoursPerDay() { return hoursPerDay; }
//
//    public void setHoursPerDay(Integer hoursPerDay) { this.hoursPerDay = hoursPerDay; }
//
//    public User getUser() { return user; }
//
//    public void setUser(User user) { this.user = user; }
//
//    public Provider getProvider() { return provider; }
//
//    public void setProvider(Provider provider) { this.provider = provider; }
//
//    public ProviderAvailability getAvailability() { return availability; }
//
//    public void setAvailability(ProviderAvailability availability) { this.availability = availability; }
//
//    public BookingStatus getStatus() { return status; }
//
//    public void setStatus(BookingStatus status) { this.status = status; }
//
//    public int getTotalDays() { return totalDays; }
//
//    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
//
//    public int getHolidays() { return holidays; }
//
//    public void setHolidays(int holidays) { this.holidays = holidays; }
//
//    public int getChargeableDays() { return chargeableDays; }
//
//    public void setChargeableDays(int chargeableDays) { this.chargeableDays = chargeableDays; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//
//    public LocalDateTime getUpdatedAt() { return updatedAt; }
//
//    public LocalDateTime getCompletedAt() { return completedAt; }
//
//    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
//
//    public LocalDate getWorkStartDate() { return workStartDate; }
//
//    public void markWorkStarted(LocalDate date) {
//        if (this.workStartDate != null) {
//            throw new IllegalStateException("Work start already set");
//        }
//        this.workStartDate = date;
//    }
//
//    public LocalDate getWorkEndDate() { return workEndDate; }
//
//    public void markWorkEnded(LocalDate date) {
//        if (this.workEndDate != null) {
//            throw new IllegalStateException("Work end already set");
//        }
//        this.workEndDate = date;
//    }
//
//    public PaymentStatus getPaymentStatus() {
//        return paymentStatus;
//    }
//
//    public void setPaymentStatus(PaymentStatus paymentStatus) {
//        this.paymentStatus = paymentStatus;
//    }
//
//    public String getStripeSessionId() {
//        return stripeSessionId;
//    }
//
//    public void setStripeSessionId(String stripeSessionId) {
//        this.stripeSessionId = stripeSessionId;
//    }
//
//    public String getStripePaymentIntent() {
//        return stripePaymentIntent;
//    }
//
//    public void setStripePaymentIntent(String stripePaymentIntent) {
//        this.stripePaymentIntent = stripePaymentIntent;
//    }
//
//    public LocalTime getBookingStartTime() {
//        return bookingStartTime;
//    }
//
//    public void setBookingStartTime(LocalTime bookingStartTime) {
//        this.bookingStartTime = bookingStartTime;
//    }
//
//    public LocalTime getBookingEndTime() {
//        return bookingEndTime;
//    }
//
//    public void setBookingEndTime(LocalTime bookingEndTime) {
//        this.bookingEndTime = bookingEndTime;
//    }
//    public void setWorkEndDate(java.time.LocalDate workEndDate) {
//        this.workEndDate = workEndDate;
//    }
//
//    public Double getWalletUsed() {
//        return walletUsed;
//    }
//
//    public void setWalletUsed(Double walletUsed) {
//        this.walletUsed = walletUsed;
//    }
//
//    public Double getFinalPayableAmount() {
//        return finalPayableAmount;
//    }
//
//    public void setFinalPayableAmount(Double finalPayableAmount) {
//        this.finalPayableAmount = finalPayableAmount;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//}
package com.homemakers.homemakers.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_services", joinColumns = @JoinColumn(name = "booking_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service")
    private Set<ServiceType> services;

    private Double walletUsed = 0.0;
    private Double finalPayableAmount;

    @Column(nullable = false)
    private double totalPrice;

    private Integer hoursPerDay;

    // ✅ ADDED — special instructions from customer at booking time
    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @ManyToOne
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne
    @JsonIgnoreProperties({"availabilities", "hibernateLazyInitializer", "handler"})
    private Provider provider;

    @OneToOne(optional = true)
    @JoinColumn(name = "availability_id", unique = true)
    private ProviderAvailability availability;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    private int totalDays;
    private int holidays;
    private int chargeableDays;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    private LocalDate workStartDate;
    private LocalDate workEndDate;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String stripeSessionId;
    private String stripePaymentIntent;

    @Column(name = "booking_start_time")
    private LocalTime bookingStartTime;

    @Column(name = "booking_end_time")
    private LocalTime bookingEndTime;

    @Column(name = "original_slot_start")
    private LocalTime originalSlotStart;

    @Column(name = "original_slot_end")
    private LocalTime originalSlotEnd;

    private boolean settlementDone = false;

    @Column(nullable = true)
    private Double penaltyApplied;
    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    @Enumerated(EnumType.STRING)
    private WalletConsentStatus walletConsentStatus = WalletConsentStatus.PENDING;
    private Double walletEligible;
    // ================= GETTERS & SETTERS =================

    public Long getId() { return id; }

    public Set<ServiceType> getServices() { return services; }
    public void setServices(Set<ServiceType> services) { this.services = services; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public Integer getHoursPerDay() { return hoursPerDay; }
    public void setHoursPerDay(Integer hoursPerDay) { this.hoursPerDay = hoursPerDay; }

    // ✅ ADDED
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }

    public ProviderAvailability getAvailability() { return availability; }
    public void setAvailability(ProviderAvailability availability) { this.availability = availability; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getHolidays() { return holidays; }
    public void setHolidays(int holidays) { this.holidays = holidays; }

    public int getChargeableDays() { return chargeableDays; }
    public void setChargeableDays(int chargeableDays) { this.chargeableDays = chargeableDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDate getWorkStartDate() { return workStartDate; }

    public void markWorkStarted(LocalDate date) {
        if (this.workStartDate != null) throw new IllegalStateException("Work start already set");
        this.workStartDate = date;
    }

    public LocalDate getWorkEndDate() { return workEndDate; }
    public void setWorkEndDate(LocalDate workEndDate) { this.workEndDate = workEndDate; }

    public void markWorkEnded(LocalDate date) {
        if (this.workEndDate != null) throw new IllegalStateException("Work end already set");
        this.workEndDate = date;
    }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }

    public String getStripePaymentIntent() { return stripePaymentIntent; }
    public void setStripePaymentIntent(String stripePaymentIntent) { this.stripePaymentIntent = stripePaymentIntent; }

    public LocalTime getBookingStartTime() { return bookingStartTime; }
    public void setBookingStartTime(LocalTime bookingStartTime) { this.bookingStartTime = bookingStartTime; }

    public LocalTime getBookingEndTime() { return bookingEndTime; }
    public void setBookingEndTime(LocalTime bookingEndTime) { this.bookingEndTime = bookingEndTime; }

    public LocalTime getOriginalSlotStart() { return originalSlotStart; }
    public void setOriginalSlotStart(LocalTime originalSlotStart) { this.originalSlotStart = originalSlotStart; }

    public LocalTime getOriginalSlotEnd() { return originalSlotEnd; }
    public void setOriginalSlotEnd(LocalTime originalSlotEnd) { this.originalSlotEnd = originalSlotEnd; }

    public Double getWalletUsed() { return walletUsed; }
    public void setWalletUsed(Double walletUsed) { this.walletUsed = walletUsed; }

    public Double getFinalPayableAmount() { return finalPayableAmount; }
    public void setFinalPayableAmount(Double finalPayableAmount) { this.finalPayableAmount = finalPayableAmount; }

    public boolean isSettlementDone() { return settlementDone; }
    public void setSettlementDone(boolean settlementDone) { this.settlementDone = settlementDone; }

    public Double getPenaltyApplied() { return penaltyApplied; }
    public void setPenaltyApplied(Double penaltyApplied) { this.penaltyApplied = penaltyApplied; }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public WalletConsentStatus getWalletConsentStatus() {
        return walletConsentStatus;
    }

    public void setWalletConsentStatus(WalletConsentStatus walletConsentStatus) {
        this.walletConsentStatus = walletConsentStatus;
    }

    public Double getWalletEligible() {
        return walletEligible;
    }

    public void setWalletEligible(Double walletEligible) {
        this.walletEligible = walletEligible;
    }
}