package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.WorkStatus;
import java.time.LocalDate;

public class ProviderAttendanceDTO {

    private Long id;
    private Long bookingId;
    private String customerName;
    private LocalDate workDate;
    private WorkStatus status;

    public ProviderAttendanceDTO(Long id,
                                 Long bookingId,
                                 String customerName,
                                 LocalDate workDate,
                                 WorkStatus status) {
        this.id = id;
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.workDate = workDate;
        this.status = status;
    }

    public Long getId() { return id; }

    public Long getBookingId() { return bookingId; }

    public String getCustomerName() { return customerName; }

    public LocalDate getWorkDate() { return workDate; }

    public WorkStatus getStatus() { return status; }
}