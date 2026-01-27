package com.homemakers.homemakers.model;

public final class BookingConstants {

    private BookingConstants() {
        // prevent instantiation
    }

    public static final int TOTAL_DAYS = 30;
    public static final int HOLIDAYS = 3;

    public static final int CHARGEABLE_DAYS = TOTAL_DAYS - HOLIDAYS; // 27
}
