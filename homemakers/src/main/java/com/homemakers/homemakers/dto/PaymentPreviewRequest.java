package com.homemakers.homemakers.dto;

public class PaymentPreviewRequest {
    private Long bookingId;
    private boolean subscriptionTaken;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public boolean isSubscriptionTaken() {
        return subscriptionTaken;
    }

    public void setSubscriptionTaken(boolean subscriptionTaken) {
        this.subscriptionTaken = subscriptionTaken;
    }

    // getters & setters
}
