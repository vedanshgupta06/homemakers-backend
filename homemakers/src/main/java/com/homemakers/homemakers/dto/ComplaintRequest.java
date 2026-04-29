package com.homemakers.homemakers.dto;

public class ComplaintRequest {

    private Long bookingId;
    private String description;
    private String severity; // LOW, MEDIUM, HIGH

    public Long getBookingId()             { return bookingId; }
    public void setBookingId(Long id)      { this.bookingId = id; }
    public String getDescription()         { return description; }
    public void setDescription(String d)   { this.description = d; }
    public String getSeverity()            { return severity; }
    public void setSeverity(String s)      { this.severity = s; }
}