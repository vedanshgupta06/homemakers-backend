package com.homemakers.homemakers.util;

import com.homemakers.homemakers.model.ServiceType;
import com.homemakers.homemakers.model.PricingType;

import java.util.Set;

public class ServiceDurationUtil {

    public static int getTotalMinutes(Set<ServiceType> services, int hoursPerDay) {

        int totalMinutes = 0;

        for (ServiceType service : services) {

            if (service.getPricingType() == PricingType.HOURLY_MONTHLY) {

                totalMinutes += hoursPerDay * 60;

            } else {

                totalMinutes += getMinutes(service);
            }
        }

        return totalMinutes;
    }

    public static int getMinutes(ServiceType service) {

        switch (service) {

            case DISH_WASHING:
                return 45;

            case CLEANING:
                return 60;

            case DUSTING:
                return 30;

            case LAUNDRY:
                return 40;

            default:
                return 30;
        }
    }
}