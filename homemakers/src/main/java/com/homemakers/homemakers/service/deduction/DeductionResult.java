package com.homemakers.homemakers.service.deduction;

import com.homemakers.homemakers.model.DeductionType;

public class DeductionResult {

    private final DeductionType deductionType;
    private final double amount;
    private final String reason;

    public DeductionResult(
            DeductionType deductionType,
            double amount,
            String reason
    ) {
        this.deductionType = deductionType;
        this.amount = amount;
        this.reason = reason;
    }

    public DeductionType getDeductionType() {
        return deductionType;
    }

    public double getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }
}
