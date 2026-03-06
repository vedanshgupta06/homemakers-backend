package com.homemakers.homemakers.dto;

import java.time.LocalDate;

public class CustomerAttendanceDTO {

    private Long id;
    private Long bookingId;
    private String providerName;
    private LocalDate workDate;
    private String status;

    public CustomerAttendanceDTO(
            Long id,
            Long bookingId,
            String providerName,
            LocalDate workDate,
            String status
    ) {
        this.id = id;
        this.bookingId = bookingId;
        this.providerName = providerName;
        this.workDate = workDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public String getProviderName() { return providerName; }
    public LocalDate getWorkDate() { return workDate; }
    public String getStatus() { return status; }
}