package com.homemakers.homemakers.model;

public enum WorkStatus {

    // System auto-generated attendance
    AUTO_PRESENT,

    // Provider explicitly confirmed presence (optional feature)
    PRESENT,

    // Provider was on approved leave
    LEAVE,

    // Platform-wide or city-wide holiday
    HOLIDAY,

    // Provider absent without approval
    ABSENT
}


