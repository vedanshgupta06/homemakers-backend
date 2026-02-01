package com.homemakers.homemakers.dto;

public class SettlementPreview {

    private long workedDays;
    private long leaveDays;
    private int earnedPaidLeaves;
    private long paidLeavesUsed;
    private long unpaidLeaves;
    private double dailySalary;
    private double potentialDeduction;

    public SettlementPreview(
            long workedDays,
            long leaveDays,
            int earnedPaidLeaves,
            long paidLeavesUsed,
            long unpaidLeaves,
            double dailySalary,
            double potentialDeduction
    ) {
        this.workedDays = workedDays;
        this.leaveDays = leaveDays;
        this.earnedPaidLeaves = earnedPaidLeaves;
        this.paidLeavesUsed = paidLeavesUsed;
        this.unpaidLeaves = unpaidLeaves;
        this.dailySalary = dailySalary;
        this.potentialDeduction = potentialDeduction;
    }

    public long getWorkedDays() {
        return workedDays;
    }

    public long getLeaveDays() {
        return leaveDays;
    }

    public int getEarnedPaidLeaves() {
        return earnedPaidLeaves;
    }

    public long getPaidLeavesUsed() {
        return paidLeavesUsed;
    }

    public long getUnpaidLeaves() {
        return unpaidLeaves;
    }

    public double getDailySalary() {
        return dailySalary;
    }

    public double getPotentialDeduction() {
        return potentialDeduction;
    }

    // getters only (no setters)
}
