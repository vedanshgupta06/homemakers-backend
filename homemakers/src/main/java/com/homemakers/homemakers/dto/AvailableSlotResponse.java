package com.homemakers.homemakers.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AvailableSlotResponse {

    private Long slotId;
    private Long providerId;
    private String providerName;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    public AvailableSlotResponse(
            Long slotId,
            Long providerId,
            String providerName,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.slotId = slotId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getSlotId() {
        return slotId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}