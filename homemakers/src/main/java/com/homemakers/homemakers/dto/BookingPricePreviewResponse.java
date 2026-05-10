package com.homemakers.homemakers.dto;

import java.util.Map;

public class BookingPricePreviewResponse {

    private double totalMonthlyPrice;   // base service price (provider receives this)
    private double platformFee;         // 5% of totalMonthlyPrice
    private double totalWithFee;        // totalMonthlyPrice + platformFee (user pays this)
    private Map<String, Double> serviceWisePrice;
    private String providerName;
    private String slotStart;
    private String slotEnd;
    private String houseSize;
    private Integer members;

    private static final double PLATFORM_FEE_RATE = 0.05;

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
        this.platformFee       = Math.round(totalMonthlyPrice * PLATFORM_FEE_RATE * 100.0) / 100.0;
        this.totalWithFee      = Math.round((totalMonthlyPrice + this.platformFee) * 100.0) / 100.0;
        this.serviceWisePrice  = serviceWisePrice;
        this.providerName      = providerName;
        this.slotStart         = slotStart;
        this.slotEnd           = slotEnd;
        this.houseSize         = houseSize;
        this.members           = members;
    }

    public double getTotalMonthlyPrice() { return totalMonthlyPrice; }
    public double getPlatformFee()       { return platformFee;       }
    public double getTotalWithFee()      { return totalWithFee;      }
    public Map<String, Double> getServiceWisePrice() { return serviceWisePrice; }
    public String getProviderName()      { return providerName;      }
    public String getSlotStart()         { return slotStart;         }
    public String getSlotEnd()           { return slotEnd;           }
    public String getHouseSize()         { return houseSize;         }
    public Integer getMembers()          { return members;           }
}