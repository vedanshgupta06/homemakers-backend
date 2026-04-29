package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.ServiceType;

public class BookingServiceRequestDTO {

    private ServiceType serviceType;

    // only meaningful for hourly services
    private Integer hours;

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }
}