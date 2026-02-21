package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;

    public BookingController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /* ======================================================
       PROVIDER – START WORK
       ====================================================== */
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('PROVIDER')")
    public Booking startWork(@PathVariable Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Booking not confirmed");
        }

        if (booking.getWorkStartDate() != null) {
            throw new RuntimeException("Work already started");
        }

        // Only block if starting BEFORE service date
        if (LocalDate.now().isBefore(booking.getAvailability().getDate())) {
            throw new RuntimeException("Cannot start before service date");
        }

        booking.markWorkStarted(LocalDate.now());
        booking.setStatus(BookingStatus.SERVICE_IN_PROGRESS);

        return bookingRepository.save(booking);
    }

    /* ======================================================
       PROVIDER – END WORK (ONLY AFTER MONTH COMPLETES)
       ====================================================== */
    @PutMapping("/{id}/end")
    @PreAuthorize("hasRole('PROVIDER')")
    public Booking endWork(@PathVariable Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.SERVICE_IN_PROGRESS) {
            throw new IllegalStateException("Work not in progress");
        }

        LocalDate expectedEnd =
                booking.getWorkStartDate().plusDays(booking.getTotalDays());

        if (LocalDate.now().isBefore(expectedEnd)) {
            throw new IllegalStateException(
                    "Cannot end before monthly cycle completes"
            );
        }

        booking.markWorkEnded(LocalDate.now());
        booking.setStatus(BookingStatus.SERVICE_DONE);

        // Full month completed
        booking.setChargeableDays(booking.getTotalDays());

        return bookingRepository.save(booking);
    }

    /* ======================================================
       ADMIN – STOP WORK EARLY (COMPLAINT / DISPUTE)
       ====================================================== */
    @PutMapping("/admin/{id}/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public Booking stopWorkEarly(@PathVariable Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.SERVICE_IN_PROGRESS) {
            throw new IllegalStateException("Service not active");
        }

        booking.markWorkEnded(LocalDate.now());
        booking.setStatus(BookingStatus.SERVICE_STOPPED_BY_ADMIN);

        long daysWorked = ChronoUnit.DAYS.between(
                booking.getWorkStartDate(),
                booking.getWorkEndDate()
        );

        booking.setChargeableDays((int) daysWorked);

        return bookingRepository.save(booking);
    }

    /* ======================================================
       ADMIN – FINALIZE BOOKING
       ====================================================== */
    @PutMapping("/admin/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public Booking finalizeBooking(@PathVariable Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.SERVICE_DONE
                && booking.getStatus() != BookingStatus.CANCELLED) {
            throw new IllegalStateException("Service not eligible for completion");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(java.time.LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    /* ======================================================
       OPTIONAL – ADMIN VIEW ALL BOOKINGS
       ====================================================== */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}