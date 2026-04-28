package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

public class BookingDetailResponse {

    private Long id;
    private Set<ServiceType> services;
    private Double walletUsed;
    private Double finalPayableAmount;
    private double totalPrice;
    private Integer hoursPerDay;
    private User user;
    private Provider provider;
    private ProviderAvailability availability;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDate workStartDate;
    private LocalDate workEndDate;
    private String paymentStatus;
    private String stripeSessionId;
    private String stripePaymentIntent;
    private LocalTime bookingStartTime;
    private LocalTime bookingEndTime;
    private boolean settlementDone;
    private Double penaltyApplied;
    private boolean rated;                          // ← NEW

    private int totalDays;
    private int chargeableDays;
    private int absent;
    private int leave;
    private int holidays;

    public BookingDetailResponse(Booking b, int totalDays, int chargeableDays, int absent, int leave, boolean rated) {
        this.id                  = b.getId();
        this.services            = b.getServices();
        this.walletUsed          = b.getWalletUsed();
        this.finalPayableAmount  = b.getFinalPayableAmount();
        this.totalPrice          = b.getTotalPrice();
        this.hoursPerDay         = b.getHoursPerDay();
        this.user                = b.getUser();
        this.provider            = b.getProvider();
        this.availability        = b.getAvailability();
        this.status              = b.getStatus();
        this.createdAt           = b.getCreatedAt();
        this.updatedAt           = b.getUpdatedAt();
        this.completedAt         = b.getCompletedAt();
        this.workStartDate       = b.getWorkStartDate();
        this.workEndDate         = b.getWorkEndDate();
        this.paymentStatus       = b.getPaymentStatus().name();
        this.stripeSessionId     = b.getStripeSessionId();
        this.stripePaymentIntent = b.getStripePaymentIntent();
        this.bookingStartTime    = b.getBookingStartTime();
        this.bookingEndTime      = b.getBookingEndTime();
        this.settlementDone      = b.isSettlementDone();
        this.penaltyApplied      = b.getPenaltyApplied();
        this.rated               = rated;           // ← NEW

        this.totalDays       = totalDays;
        this.chargeableDays  = chargeableDays;
        this.absent          = absent;
        this.leave           = leave;
        this.holidays        = leave;
    }

    // all existing getters unchanged...
    public Long getId()                           { return id; }
    public Set<ServiceType> getServices()         { return services; }
    public Double getWalletUsed()                 { return walletUsed; }
    public Double getFinalPayableAmount()         { return finalPayableAmount; }
    public double getTotalPrice()                 { return totalPrice; }
    public Integer getHoursPerDay()               { return hoursPerDay; }
    public User getUser()                         { return user; }
    public Provider getProvider()                 { return provider; }
    public ProviderAvailability getAvailability() { return availability; }
    public BookingStatus getStatus()              { return status; }
    public LocalDateTime getCreatedAt()           { return createdAt; }
    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public LocalDateTime getCompletedAt()         { return completedAt; }
    public LocalDate getWorkStartDate()           { return workStartDate; }
    public LocalDate getWorkEndDate()             { return workEndDate; }
    public String getPaymentStatus()              { return paymentStatus; }
    public String getStripeSessionId()            { return stripeSessionId; }
    public String getStripePaymentIntent()        { return stripePaymentIntent; }
    public LocalTime getBookingStartTime()        { return bookingStartTime; }
    public LocalTime getBookingEndTime()          { return bookingEndTime; }
    public boolean isSettlementDone()             { return settlementDone; }
    public Double getPenaltyApplied()             { return penaltyApplied; }
    public int getTotalDays()                     { return totalDays; }
    public int getChargeableDays()                { return chargeableDays; }
    public int getAbsent()                        { return absent; }
    public int getLeave()                         { return leave; }
    public int getHolidays()                      { return holidays; }
    public boolean isRated()                      { return rated; } // ← NEW
}