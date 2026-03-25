package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.PricingType;
import com.homemakers.homemakers.model.ServicePricing;
import com.homemakers.homemakers.model.ServiceType;
import org.springframework.stereotype.Service;

@Service
public class BookingCalculationService {

    /* =====================================
       MEMBER MULTIPLIER
       ===================================== */
    public double getMemberMultiplier(int members) {

        if (members <= 4) return 1.0;
        if (members <= 6) return 1.25;
        if (members <= 8) return 1.5;

        return 1.75;
    }

    /* =====================================
       HOUSE SIZE MULTIPLIER
       ===================================== */
    public double getHouseMultiplier(String houseSize) {

        switch (houseSize) {
            case "1BHK": return 1.0;
            case "2BHK": return 1.3;
            case "3BHK": return 1.6;
            case "4BHK_PLUS": return 2.0;
            default: return 1.0;
        }
    }

    /* =====================================
       PRICE CALCULATION
       ===================================== */
    public double calculatePrice(ServicePricing pricing,
                                 ServiceType service,
                                 int members,
                                 String houseSize,
                                 Integer hoursPerDay) {

        /* HOURLY SERVICES */
        if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {

            if (hoursPerDay == null || hoursPerDay <= 0) {
                throw new RuntimeException("Hours per day required for hourly services");
            }

            return pricing.getPricePerHour() * hoursPerDay;
        }

        /* FLAT MONTHLY SERVICES */

        double basePrice = pricing.getMonthlyRate();

        switch (service) {

            case DISH_WASHING:
            case LAUNDRY:
                return basePrice * getMemberMultiplier(members);

            case CLEANING:
            case DUSTING:
                return basePrice * getHouseMultiplier(houseSize);

            default:
                return basePrice;
        }
    }
}