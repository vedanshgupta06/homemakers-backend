package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.BookingStatus;

public class BookingResponse {

    private Long bookingId;
    private BookingStatus status;

    public BookingResponse(Long bookingId, BookingStatus status) {
        this.bookingId = bookingId;
        this.status = status;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public BookingStatus getStatus() {
        return status;
    }
}
