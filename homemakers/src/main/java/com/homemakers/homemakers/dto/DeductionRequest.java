package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.DeductionType;

public class DeductionRequest {
    private double amount;
    private DeductionType type;
    private String reason;

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public DeductionType getType() { return type; }
    public void setType(DeductionType type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
