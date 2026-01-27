package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.ServiceType;
import java.util.Set;

public class BookingPricePreviewRequest {

    private Long providerId;
    private Set<ServiceType> services;

    // ONLY needed for HOURLY_MONTHLY services
    private Integer hoursPerDay;

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public Set<ServiceType> getServices() {
        return services;
    }

    public void setServices(Set<ServiceType> services) {
        this.services = services;
    }

    public Integer getHoursPerDay() {
        return hoursPerDay;
    }

    public void setHoursPerDay(Integer hoursPerDay) {
        this.hoursPerDay = hoursPerDay;
    }
}
