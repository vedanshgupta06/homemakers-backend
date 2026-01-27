package com.homemakers.homemakers.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AvailabilityRequest {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

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
