package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.BookingStatus;
import com.homemakers.homemakers.model.ServiceType;

import java.util.List;

public class BookingResponse {

    private Long bookingId;
    private BookingStatus status;

    private String customerName;
    private String customerPhone;    // ✅ null for PENDING/REJECTED/CANCELLED, real number after CONFIRMED
    private String serviceAddress;   // ✅ from user.getAddress()
    private String customerNote;     // ✅ from booking.getCustomerNote()

    private String serviceDate;
    private String startTime;
    private String endTime;

    private String paymentStatus;
    private Double totalAmount;      // ✅ from booking.getFinalPayableAmount()

    private int totalDays;
    private int chargeableDays;
    private int holidays;

    private List<ServiceType> services;

    public BookingResponse(Long bookingId,
                           BookingStatus status,
                           String customerName,
                           String customerPhone,
                           String serviceAddress,
                           String customerNote,
                           String serviceDate,
                           List<ServiceType> services,
                           String startTime,
                           String endTime,
                           String paymentStatus,
                           Double totalAmount,
                           int totalDays,
                           int chargeableDays,
                           int holidays) {

        this.bookingId = bookingId;
        this.status = status;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.serviceAddress = serviceAddress;
        this.customerNote = customerNote;
        this.serviceDate = serviceDate;
        this.services = services;
        this.startTime = startTime;
        this.endTime = endTime;
        this.paymentStatus = paymentStatus;
        this.totalAmount = totalAmount;
        this.totalDays = totalDays;
        this.chargeableDays = chargeableDays;
        this.holidays = holidays;
    }

    // ── Getters ──────────────────────────────────────────────

    public Long getBookingId() { return bookingId; }
    public BookingStatus getStatus() { return status; }

    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getServiceAddress() { return serviceAddress; }
    public String getCustomerNote() { return customerNote; }

    public String getServiceDate() { return serviceDate; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public String getPaymentStatus() { return paymentStatus; }
    public Double getTotalAmount() { return totalAmount; }

    public int getTotalDays() { return totalDays; }
    public int getChargeableDays() { return chargeableDays; }
    public int getHolidays() { return holidays; }

    public List<ServiceType> getServices() { return services; }

    // ── Setters ──────────────────────────────────────────────

    public void setServices(List<ServiceType> services) { this.services = services; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
}