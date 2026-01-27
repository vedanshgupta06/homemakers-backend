//package com.homemakers.homemakers.controller;
//
//import com.homemakers.homemakers.dto.BookingRequest;
//import com.homemakers.homemakers.model.Booking;
//import com.homemakers.homemakers.model.BookingStatus;
//import com.homemakers.homemakers.service.BookingService;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//@RestController
//@RequestMapping("/api/bookings")
//public class BookingController {
//
//    private final BookingService bookingService;
//
//    public BookingController(BookingService bookingService) {
//        this.bookingService = bookingService;
//    }
//
//    // =====================
//    // USER creates booking
//    // =====================
//    @PostMapping
//    @PreAuthorize("hasRole('USER')")
//    public Booking createBooking(@RequestBody BookingRequest request) {
//
//        String email = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        return bookingService.createBooking(request, email);
//    }
//
//    // =====================
//    // PROVIDER views bookings
//    // =====================
//    @GetMapping("/provider")
//    @PreAuthorize("hasRole('PROVIDER')")
//    public List<Booking> getProviderBookings() {
//
//        String email = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        return bookingService.getProviderBookings(email);
//    }
//    // PROVIDER accepts booking
//// =====================
//    @PutMapping("/provider/{bookingId}/accept")
//    @PreAuthorize("hasRole('PROVIDER')")
//    public Booking acceptBooking(@PathVariable Long bookingId) {
//
//        String email = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        return bookingService.acceptBooking(bookingId, email);
//    }
//
//    // =====================
//// PROVIDER rejects booking
//// =====================
//    @PutMapping("/provider/{bookingId}/reject")
//    @PreAuthorize("hasRole('PROVIDER')")
//    public Booking rejectBooking(@PathVariable Long bookingId) {
//
//        String email = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        return bookingService.rejectBooking(bookingId, email);
//    }
//    @PutMapping("/provider/{id}/status")
//    @PreAuthorize("hasRole('PROVIDER')")
//    public Booking updateBookingStatus(
//            @PathVariable Long id,
//            @RequestParam BookingStatus status
//    ) {
//        String email = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        return bookingService.updateBookingStatus(id, status, email);
//    }
//    @PutMapping("/provider/{bookingId}/complete")
//    @PreAuthorize("hasRole('PROVIDER')")
//    public Booking completeBooking(@PathVariable Long bookingId) {
//
//        String email = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        return bookingService.completeBooking(bookingId, email);
//    }
//    // =====================
//// USER views own bookings
//// =====================
//    @GetMapping
//    @PreAuthorize("hasRole('USER')")
//    public List<Booking> getUserBookings() {
//
//        String email = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        return bookingService.getUserBookings(email);
//    }
//}

package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.BookingPricePreviewRequest;
import com.homemakers.homemakers.dto.BookingPricePreviewResponse;
import com.homemakers.homemakers.dto.BookingRequest;
import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.model.BookingStatus;
import com.homemakers.homemakers.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // USER creates booking
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Booking createBooking(@RequestBody BookingRequest request) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return bookingService.createBooking(request, email);
    }

    // USER views own bookings
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public List<Booking> getMyBookings() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return bookingService.getUserBookings(email);
    }

    // PROVIDER views bookings
    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public List<Booking> getProviderBookings() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return bookingService.getProviderBookings(email);
    }


    @PostMapping("/preview")
    @PreAuthorize("hasRole('USER')")
    public BookingPricePreviewResponse previewBookingPrice(
            @RequestBody BookingPricePreviewRequest request
    ) {
        return bookingService.previewBookingPrice(request);
    }

}
