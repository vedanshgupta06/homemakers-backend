package com.homemakers.homemakers.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "provider_availability",
        indexes = {
                @Index(name = "idx_provider_date", columnList = "provider_id,date"),
                @Index(name = "idx_date_active", columnList = "date,active")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"provider_id", "date", "start_time", "end_time"}
                )
        }
)
public class ProviderAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    @JsonIgnoreProperties({"availabilities", "hibernateLazyInitializer", "handler"})
    private Provider provider;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(name = "active")
    private Boolean active;

    // =============================
    // BOOKING RANGE FIELDS
    // Set when a slot is locked by an active booking.
    // bookingWorkStart = anchor date (date booking was originally placed)
    // bookingWorkEnd   = anchor date + 30 days (or actual termination date)
    // bookingCustomerName = name of the customer who booked
    // All three are null for free (active) slots and cleared on cancellation/termination.
    // =============================

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "booking_work_start")
    private LocalDate bookingWorkStart;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "booking_work_end")
    private LocalDate bookingWorkEnd;

    @Column(name = "booking_customer_name")
    private String bookingCustomerName;

    // =============================
    // VALIDATION
    // =============================

    @PrePersist
    @PreUpdate
    public void validateTimes() {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException(
                    "Slot start time must be before end time"
            );
        }
    }

    // =============================
    // HELPER
    // =============================

    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    // =============================
    // GETTERS / SETTERS
    // =============================

    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getBookingWorkStart() {
        return bookingWorkStart;
    }

    public void setBookingWorkStart(LocalDate bookingWorkStart) {
        this.bookingWorkStart = bookingWorkStart;
    }

    public LocalDate getBookingWorkEnd() {
        return bookingWorkEnd;
    }

    public void setBookingWorkEnd(LocalDate bookingWorkEnd) {
        this.bookingWorkEnd = bookingWorkEnd;
    }

    public String getBookingCustomerName() {
        return bookingCustomerName;
    }

    public void setBookingCustomerName(String bookingCustomerName) {
        this.bookingCustomerName = bookingCustomerName;
    }
}