package com.homemakers.homemakers.model;

public enum BookingStatus {

    // Customer created booking, waiting for provider
    PENDING,

    // Provider accepted, work not started yet
    CONFIRMED,

    // System-controlled: work has started (attendance running)
    SERVICE_IN_PROGRESS,

    // Provider has signaled work is finished
    SERVICE_DONE,

    // System / Admin finalized booking
    COMPLETED,

    // Booking cancelled before completion
    CANCELLED,

    SERVICE_STOPPED_BY_ADMIN
}