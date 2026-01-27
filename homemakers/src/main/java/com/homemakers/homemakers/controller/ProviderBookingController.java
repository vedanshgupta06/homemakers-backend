package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderBookingController {

    private final BookingService bookingService;

    public ProviderBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ============================
    // ACCEPT BOOKING
    // ============================
    @PutMapping("/{bookingId}/accept")
    public Booking acceptBooking(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        return bookingService.acceptBooking(
                bookingId,
                authentication.getName()
        );
    }

    // ============================
    // REJECT BOOKING
    // ============================
    @PutMapping("/{bookingId}/reject")
    public Booking rejectBooking(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        return bookingService.rejectBooking(
                bookingId,
                authentication.getName()
        );
    }

    // ============================
    // COMPLETE BOOKING (🔥 IMPORTANT)
    // ============================
    @PutMapping("/{bookingId}/complete")
    public Booking completeBooking(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        return bookingService.completeBooking(
                bookingId,
                authentication.getName()
        );
    }
}
