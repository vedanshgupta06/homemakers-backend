package com.homemakers.homemakers.dto;

public class AdminPayoutDTO {

    private Long id;
    private Long providerId;
    private String payoutMonth;
    private double totalEarnings;
    private double totalDeductions;
    private double netPayout;
    private String status;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public String getPayoutMonth() { return payoutMonth; }
    public void setPayoutMonth(String payoutMonth) { this.payoutMonth = payoutMonth; }

    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }

    public double getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(double totalDeductions) { this.totalDeductions = totalDeductions; }

    public double getNetPayout() { return netPayout; }
    public void setNetPayout(double netPayout) { this.netPayout = netPayout; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
