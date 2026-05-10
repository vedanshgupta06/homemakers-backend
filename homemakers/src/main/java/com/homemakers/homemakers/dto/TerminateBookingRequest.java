package com.homemakers.homemakers.dto;

/**
 * Optional request body for the terminate booking endpoint.
 * The body itself (and the reason field) may be null — the controller
 * handles both cases gracefully.
 */
public class TerminateBookingRequest {

    private String reason;

    public TerminateBookingRequest() {}

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}