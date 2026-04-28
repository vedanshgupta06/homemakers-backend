package com.homemakers.homemakers.dto;

import java.time.LocalDate;
import java.util.List;

public class BookingPreviewRequestDTO {

    private Long providerId;
    private Long availabilityId;

    private String houseSize;
    private Integer members;

    private LocalDate startDate;

    private List<BookingServiceRequestDTO> services;

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public Long getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(Long availabilityId) {
        this.availabilityId = availabilityId;
    }

    public String getHouseSize() {
        return houseSize;
    }

    public void setHouseSize(String houseSize) {
        this.houseSize = houseSize;
    }

    public Integer getMembers() {
        return members;
    }

    public void setMembers(Integer members) {
        this.members = members;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public List<BookingServiceRequestDTO> getServices() {
        return services;
    }

    public void setServices(List<BookingServiceRequestDTO> services) {
        this.services = services;
    }

    // getters setters
}
