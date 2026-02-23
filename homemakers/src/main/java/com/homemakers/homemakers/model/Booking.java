package com.homemakers.homemakers.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"availability_id"}))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_services", joinColumns = @JoinColumn(name = "booking_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service")
    private Set<ServiceType> services;

    @Column(nullable = false)
    private double totalPrice; // monthly locked price

    private Integer hoursPerDay; // 🔥 REQUIRED for HOURLY_MONTHLY

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Provider provider;

    @OneToOne(optional = false)
    @JoinColumn(name = "availability_id", unique = true)
    private ProviderAvailability availability;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    private int totalDays ;
    private int holidays ;
    private int chargeableDays ;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDate workStartDate;   // set once
    private LocalDate workEndDate;     // optional, system set
    private LocalDateTime getCreatedAt;


    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<ServiceType> getServices() {
        return services;
    }

    public void setServices(Set<ServiceType> services) {
        this.services = services;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getHoursPerDay() {
        return hoursPerDay;
    }

    public void setHoursPerDay(Integer hoursPerDay) {
        this.hoursPerDay = hoursPerDay;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public ProviderAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(ProviderAvailability availability) {
        this.availability = availability;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getHolidays() {
        return holidays;
    }

    public void setHolidays(int holidays) {
        this.holidays = holidays;
    }

    public int getChargeableDays() {
        return chargeableDays;
    }

    public void setChargeableDays(int chargeableDays) {
        this.chargeableDays = chargeableDays;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDate getWorkStartDate() {
        return workStartDate;
    }

    public void markWorkStarted(LocalDate date) {
        if (this.workStartDate != null) {
            throw new IllegalStateException("Work start already set");
        }
        this.workStartDate = date;
    }

    public LocalDate getWorkEndDate() {
        return workEndDate;
    }

    public void markWorkEnded(LocalDate date) {
        if (this.workEndDate != null) {
            throw new IllegalStateException("Work end already set");
        }
        this.workEndDate = date;
    }

    public LocalDateTime getGetCreatedAt() {
        return getCreatedAt;
    }

    public void setGetCreatedAt(LocalDateTime getCreatedAt) {
        this.getCreatedAt = getCreatedAt;
    }

    // getters/setters
}