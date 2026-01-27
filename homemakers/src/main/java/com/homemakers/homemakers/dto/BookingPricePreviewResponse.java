package com.homemakers.homemakers.dto;

import java.util.Map;

public class BookingPricePreviewResponse {

    private double totalMonthlyPrice;
    private Map<String, Double> serviceWisePrice;

    public BookingPricePreviewResponse(
            double totalMonthlyPrice,
            Map<String, Double> serviceWisePrice
    ) {
        this.totalMonthlyPrice = totalMonthlyPrice;
        this.serviceWisePrice = serviceWisePrice;
    }

    public double getTotalMonthlyPrice() {
        return totalMonthlyPrice;
    }

    public Map<String, Double> getServiceWisePrice() {
        return serviceWisePrice;
    }
}
