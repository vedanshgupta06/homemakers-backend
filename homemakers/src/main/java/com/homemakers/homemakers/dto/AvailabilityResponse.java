package com.homemakers.homemakers.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class AvailabilityResponse {

    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime endTime;

    private boolean active;
    private long durationMinutes;

    // Only populated when active=false (slot is booked)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate bookingWorkStart;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate bookingWorkEnd;

    private String bookingCustomerName;

    public AvailabilityResponse(Long id,
                                LocalDate date,
                                LocalTime startTime,
                                LocalTime endTime,
                                boolean active,
                                long durationMinutes,
                                LocalDate bookingWorkStart,
                                LocalDate bookingWorkEnd,
                                String bookingCustomerName) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
        this.durationMinutes = durationMinutes;
        this.bookingWorkStart = bookingWorkStart;
        this.bookingWorkEnd = bookingWorkEnd;
        this.bookingCustomerName = bookingCustomerName;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isActive() { return active; }
    public long getDurationMinutes() { return durationMinutes; }
    public LocalDate getBookingWorkStart() { return bookingWorkStart; }
    public LocalDate getBookingWorkEnd() { return bookingWorkEnd; }
    public String getBookingCustomerName() { return bookingCustomerName; }
}