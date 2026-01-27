package com.homemakers.homemakers.dto;

import java.util.List;
public class WeeklySummaryResponse {

    private Long bookingId;
    private String serviceName;
    private double monthlyAmount;
    private double weeklyAmount;
    private List<WeekStatus> weeks;

    public WeeklySummaryResponse(
            Long bookingId,
            String serviceName,
            double monthlyAmount,
            double weeklyAmount,
            List<WeekStatus> weeks
    ) {
        this.bookingId = bookingId;
        this.serviceName = serviceName;
        this.monthlyAmount = monthlyAmount;
        this.weeklyAmount = weeklyAmount;
        this.weeks = weeks;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getMonthlyAmount() {
        return monthlyAmount;
    }

    public double getWeeklyAmount() {
        return weeklyAmount;
    }

    public List<WeekStatus> getWeeks() {
        return weeks;
    }

    public static class WeekStatus {
        private int weekNo;
        private double amount;
        private String status;

        public WeekStatus(int weekNo, double amount, String status) {
            this.weekNo = weekNo;
            this.amount = amount;
            this.status = status;
        }

        public int getWeekNo() { return weekNo; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
    }

    // getters
}
