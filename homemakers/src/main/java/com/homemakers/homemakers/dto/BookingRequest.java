package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.ServiceType;
import java.util.Set;

import java.util.List;

public class BookingRequest {

    private Long availabilityId;
    private List<ServiceType> services;

    // Required ONLY if any service is HOURLY_MONTHLY
    private Integer hoursPerDay;

    public Long getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(Long availabilityId) {
        this.availabilityId = availabilityId;
    }

    public List<ServiceType> getServices() {
        return services;
    }

    public void setServices(List<ServiceType> services) {
        this.services = services;
    }

    public Integer getHoursPerDay() {
        return hoursPerDay;
    }

    public void setHoursPerDay(Integer hoursPerDay) {
        this.hoursPerDay = hoursPerDay;
    }
}
