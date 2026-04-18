package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.*;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ReviewRepository;
import com.homemakers.homemakers.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.homemakers.homemakers.repository.ProviderWorkLogRepository;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ProviderWorkLogRepository workLogRepository;

    public BookingController(BookingService bookingService,
                             BookingRepository bookingRepository,ProviderWorkLogRepository workLogRepository,
                             ReviewRepository reviewRepository) {

        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.workLogRepository = workLogRepository;
    }
/* ======================================================
   PROVIDER – ACCEPT BOOKING
   ====================================================== */

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('PROVIDER')")
    public Booking acceptBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return bookingService.acceptBooking(id, authentication.getName());
    }

/* ======================================================
   PROVIDER – REJECT BOOKING
   ====================================================== */

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('PROVIDER')")
    public Booking rejectBooking(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return bookingService.rejectBooking(id, authentication.getName());
    }
    /* ======================================================
       PROVIDER – START WORK
       ====================================================== */

    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('PROVIDER')")
    public Booking startWork(@PathVariable Long id,
                             Authentication authentication) {

        return bookingService.startWork(id, authentication.getName());
    }

    /* ======================================================
       PROVIDER – END WORK
       ====================================================== */

//    @PutMapping("/{id}/end")
//    @PreAuthorize("hasRole('PROVIDER')")
//    public Booking endWork(@PathVariable Long id,
//                           Authentication authentication) {
//
//        return bookingService.endWork(id, authentication.getName());
//    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('PROVIDER','USER','ADMIN')")
    public Booking terminateBooking(@PathVariable Long id) {

        return bookingService.terminateBooking(id);
    }
    /* ======================================================
       ADMIN – STOP WORK EARLY
       ====================================================== */

//    @PutMapping("/admin/{id}/stop")
//    @PreAuthorize("hasRole('ADMIN')")
//    public Booking stopWorkEarly(@PathVariable Long id) {
//
//        return bookingService.stopWorkEarly(id);
//    }

    /* ======================================================
       ADMIN – FINALIZE BOOKING
       ====================================================== */

    @PutMapping("/admin/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public Booking finalizeBooking(@PathVariable Long id) {

        return bookingService.finalizeBooking(id);
    }

    /* ======================================================
       USER – VIEW MY BOOKINGS
       ====================================================== */

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public List<Booking> getUserBookings(Authentication authentication) {

        return bookingService.getUserBookings(authentication.getName());
    }

    /* ======================================================
       PROVIDER – VIEW BOOKINGS
       ====================================================== */

//    @GetMapping("/provider")
//    @PreAuthorize("hasRole('PROVIDER')")
//    public List<Booking> getProviderBookings(Authentication authentication) {
//
//        return bookingService.getProviderBookings(authentication.getName());
//    }
@GetMapping("/provider")
@PreAuthorize("hasRole('PROVIDER')")
public List<BookingResponse> getProviderBookings(Authentication authentication) {

    List<Booking> bookings =
            bookingService.getProviderBookings(authentication.getName());

    return bookings.stream().map(booking -> {

        List<ProviderWorkLog> logs =
                workLogRepository.findByBookingId(booking.getId());

        int totalDays = 0;
        int chargeableDays = 0;
        int holidays = 0;

        for (ProviderWorkLog log : logs) {

            WorkStatus status = log.getStatus();

            if (status == WorkStatus.REJECTED) continue;

            totalDays++;

            if (status == WorkStatus.PRESENT ||
                    status == WorkStatus.CONFIRMED_PRESENT) {

                chargeableDays++;
            }

            else if (status == WorkStatus.LEAVE) {
                holidays++;
            }
        }

        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getUser() != null ? booking.getUser().getName() : "N/A",
                booking.getAvailability() != null ? booking.getAvailability().getDate().toString() : "-",
                new java.util.ArrayList<>(booking.getServices()),
                booking.getBookingStartTime() != null ? booking.getBookingStartTime().toString() : "-",
                booking.getBookingEndTime() != null ? booking.getBookingEndTime().toString() : "Ongoing",
                booking.getPaymentStatus() != null
                        ? booking.getPaymentStatus().name()
                        : "PENDING",
                totalDays,
                chargeableDays,
                totalDays-chargeableDays
        );

    }).toList();
}
    /* ======================================================
       ADMIN – VIEW ALL BOOKINGS
       ====================================================== */

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Booking> getAllBookings() {

        return bookingService.getAllBookings();
    }

    /* ======================================================
       USER – BOOKINGS WAITING FOR PAYMENT
       ====================================================== */

    @GetMapping("/user/payment-required")
    @PreAuthorize("hasRole('USER')")
    public List<Booking> getPaymentRequiredBookings(Authentication auth) {

        return bookingRepository
                .findByUser_EmailAndPaymentStatus(
                        auth.getName(),
                        PaymentStatus.PAYMENT_REQUIRED
                );
    }
    /* ======================================================
   USER – PRICE PREVIEW
   ====================================================== */

    @PostMapping("/preview")
    @PreAuthorize("hasRole('USER')")
    public BookingPricePreviewResponse previewBooking(
            @RequestBody BookingPreviewRequestDTO request
    ) {
        return bookingService.previewBookingPrice(request);
    }
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Booking createBooking(
            @RequestBody BookingPreviewRequestDTO request,
            Authentication authentication
    ) {
        return bookingService.createBooking(request, authentication.getName());
    }
    @PostMapping("/provider-options")
    public List<ProviderOptionDTO> getProviderOptions(
            @RequestBody BookingPreviewRequestDTO request
    ) {
        return bookingService.getProviderOptions(request);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','PROVIDER','ADMIN')")
    public BookingDetailResponse getBookingById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        List<ProviderWorkLog> logs = workLogRepository.findByBookingId(id);

        int totalDays = 0, chargeableDays = 0, absent = 0, leave = 0;

        for (ProviderWorkLog log : logs) {
            if (log.getStatus() == WorkStatus.REJECTED) continue;
            totalDays++;
            switch (log.getStatus()) {
                case PRESENT, CONFIRMED_PRESENT -> chargeableDays++;
                case ABSENT                     -> absent++;
                case LEAVE                      -> leave++;
            }
        }
        boolean rated = reviewRepository.existsByBooking_Id(booking.getId());
        return new BookingDetailResponse(booking, totalDays, chargeableDays, absent, leave,rated);
    }

    /* ======================================================
       USER – GET WORK LOGS FOR A BOOKING
       ====================================================== */
    @GetMapping("/{id}/work-logs")
    @PreAuthorize("hasAnyRole('USER','PROVIDER','ADMIN')")
    public List<ProviderWorkLog> getWorkLogs(@PathVariable Long id) {
        return workLogRepository.findByBookingId(id);
    }

}