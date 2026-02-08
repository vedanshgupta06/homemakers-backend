package com.homemakers.homemakers.dto;

public class PaymentPreviewResponse {

    private double serviceAmount;
    private double platformFee;
    private boolean subscriptionAllowed;
    private boolean subscriptionTaken;
    private double totalPayable;

    public PaymentPreviewResponse(
            double serviceAmount,
            double platformFee,
            boolean subscriptionAllowed,
            boolean subscriptionTaken,
            double totalPayable
    ) {
        this.serviceAmount = serviceAmount;
        this.platformFee = platformFee;
        this.subscriptionAllowed = subscriptionAllowed;
        this.subscriptionTaken = subscriptionTaken;
        this.totalPayable = totalPayable;
    }

    public double getServiceAmount() {
        return serviceAmount;
    }

    public double getPlatformFee() {
        return platformFee;
    }

    public boolean isSubscriptionAllowed() {
        return subscriptionAllowed;
    }

    public double getTotalPayable() {
        return totalPayable;
    }

    public boolean isSubscriptionTaken() {
        return subscriptionTaken;
    }

    // getters
}
