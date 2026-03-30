package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.BookingStatus;
import com.homemakers.homemakers.model.ServiceType;

import java.util.List;

public class BookingResponse {

    private Long bookingId;
    private BookingStatus status;

    private String customerName;
    private String serviceDate;
    private String startTime;
    private String endTime;

    // ✅ IMPORTANT FIELD
    private String paymentStatus;

    private int totalDays;
    private int chargeableDays;
    private int holidays;

    private List<ServiceType> services;

    // ✅ UPDATED CONSTRUCTOR (WITH paymentStatus)
    public BookingResponse(Long bookingId,
                           BookingStatus status,
                           String customerName,
                           String serviceDate,
                           List<ServiceType> services,
                           String startTime,
                           String endTime,
                           String paymentStatus,   // 🔥 ADDED
                           int totalDays,
                           int chargeableDays,
                           int holidays) {

        this.bookingId = bookingId;
        this.status = status;
        this.customerName = customerName;
        this.serviceDate = serviceDate;
        this.services = services;
        this.startTime = startTime;
        this.endTime = endTime;
        this.paymentStatus = paymentStatus; // 🔥 SET HERE
        this.totalDays = totalDays;
        this.chargeableDays = chargeableDays;
        this.holidays = holidays;
    }

    public Long getBookingId() { return bookingId; }
    public BookingStatus getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public String getServiceDate() { return serviceDate; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getTotalDays() { return totalDays; }
    public int getChargeableDays() { return chargeableDays; }
    public int getHolidays() { return holidays; }

    public List<ServiceType> getServices() {
        return services;
    }

    public void setServices(List<ServiceType> services) {
        this.services = services;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}