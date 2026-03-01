package com.homemakers.homemakers.dto;

import java.time.LocalDateTime;

public class WalletSummaryDTO {

    private double available;
    private double requested;
    private double paid;
    private LocalDateTime lastPayoutDate;
    private boolean canWithdraw;
    private LocalDateTime nextEligibleWithdrawalDate;
    public double getAvailable() { return available; }
    public void setAvailable(double available) { this.available = available; }

    public double getRequested() { return requested; }
    public void setRequested(double requested) { this.requested = requested; }

    public double getPaid() { return paid; }
    public void setPaid(double paid) { this.paid = paid; }

    public LocalDateTime getLastPayoutDate() { return lastPayoutDate; }
    public void setLastPayoutDate(LocalDateTime lastPayoutDate) {
        this.lastPayoutDate = lastPayoutDate;
    }

    public boolean isCanWithdraw() {
        return canWithdraw;
    }

    public LocalDateTime getNextEligibleWithdrawalDate() {
        return nextEligibleWithdrawalDate;
    }


    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }

    public void setNextEligibleWithdrawalDate(LocalDateTime nextEligibleWithdrawalDate) {
        this.nextEligibleWithdrawalDate = nextEligibleWithdrawalDate;
    }
}
