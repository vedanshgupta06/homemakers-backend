package com.homemakers.homemakers.dto;

import java.time.LocalDateTime;

public class WalletSummaryDTO {

    private double available;
    private double requested;
    private double paid;
    private LocalDateTime lastPayoutDate;

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
}
