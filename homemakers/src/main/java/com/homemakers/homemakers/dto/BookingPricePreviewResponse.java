package com.homemakers.homemakers.dto;

import java.util.Map;

//public class BookingPricePreviewResponse {
//
//    private double totalMonthlyPrice;
//    private Map<String, Double> serviceWisePrice;
//
//    public BookingPricePreviewResponse(
//            double totalMonthlyPrice,
//            Map<String, Double> serviceWisePrice
//    ) {
//        this.totalMonthlyPrice = totalMonthlyPrice;
//        this.serviceWisePrice = serviceWisePrice;
//    }
//
//    public double getTotalMonthlyPrice() {
//        return totalMonthlyPrice;
//    }
//
//    public Map<String, Double> getServiceWisePrice() {
//        return serviceWisePrice;
//    }
//}
public class BookingPricePreviewResponse {

    private double totalMonthlyPrice;
    private Map<String, Double> serviceWisePrice;

    private String providerName;
    private String slotStart;
    private String slotEnd;
    private String houseSize;
    private Integer members;

    public BookingPricePreviewResponse(
            double totalMonthlyPrice,
            Map<String, Double> serviceWisePrice,
            String providerName,
            String slotStart,
            String slotEnd,
            String houseSize,
            Integer members
    ) {
        this.totalMonthlyPrice = totalMonthlyPrice;
        this.serviceWisePrice = serviceWisePrice;
        this.providerName = providerName;
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.houseSize = houseSize;
        this.members = members;
    }

    public double getTotalMonthlyPrice() { return totalMonthlyPrice; }
    public Map<String, Double> getServiceWisePrice() { return serviceWisePrice; }
    public String getProviderName() { return providerName; }
    public String getSlotStart() { return slotStart; }
    public String getSlotEnd() { return slotEnd; }
    public String getHouseSize() { return houseSize; }
    public Integer getMembers() { return members; }
}