package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.*;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ReviewRepository;
import com.homemakers.homemakers.service.BookingService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService            bookingService;
    private final BookingRepository         bookingRepository;
    private final ReviewRepository          reviewRepository;
    private final ProviderWorkLogRepository workLogRepository;
    private final BookingMapper             bookingMapper;

    private static final Set<BookingStatus> PHONE_VISIBLE_STATUSES = Set.of(
            BookingStatus.CONFIRMED,
            BookingStatus.SERVICE_IN_PROGRESS,
            BookingStatus.COMPLETED,
            BookingStatus.TERMINATED
    );

    public BookingController(BookingService bookingService,
                             BookingRepository bookingRepository,
                             ProviderWorkLogRepository workLogRepository,
                             ReviewRepository reviewRepository,
                             BookingMapper bookingMapper) {
        this.bookingService    = bookingService;
        this.bookingRepository = bookingRepository;
        this.reviewRepository  = reviewRepository;
        this.workLogRepository = workLogRepository;
        this.bookingMapper     = bookingMapper;
    }

    /* ======================================================
       PROVIDER – ACCEPT BOOKING
       ====================================================== */
    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('PROVIDER')")
    @Transactional
    public ResponseEntity<?> acceptBooking(@PathVariable Long id, Authentication authentication) {
        Booking b = bookingService.acceptBooking(id, authentication.getName());
        return ResponseEntity.ok(bookingMapper.toMap(b));
    }

    /* ======================================================
       PROVIDER – REJECT BOOKING
       ====================================================== */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('PROVIDER')")
    @Transactional
    public ResponseEntity<?> rejectBooking(@PathVariable Long id, Authentication authentication) {
        Booking b = bookingService.rejectBooking(id, authentication.getName());
        return ResponseEntity.ok(bookingMapper.toMap(b));
    }

    /* ======================================================
       PROVIDER – START WORK
       ====================================================== */
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('PROVIDER')")
    @Transactional
    public ResponseEntity<?> startWork(@PathVariable Long id, Authentication authentication) {
        Booking b = bookingService.startWork(id, authentication.getName());
        return ResponseEntity.ok(bookingMapper.toMap(b));
    }

    /* ======================================================
       PROVIDER / USER / ADMIN – TERMINATE BOOKING
       ====================================================== */
    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('PROVIDER','USER','ADMIN')")
    @Transactional
    public ResponseEntity<?> terminateBooking(
            @PathVariable Long id,
            @RequestBody(required = false) TerminateBookingRequest request) {
        String reason = (request != null) ? request.getReason() : null;
        Booking b = bookingService.terminateBooking(id, reason);
        return ResponseEntity.ok(bookingMapper.toMap(b));
    }

    /* ======================================================
       ADMIN – FINALIZE BOOKING
       ====================================================== */
    @PutMapping("/admin/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> finalizeBooking(@PathVariable Long id) {
        Booking b = bookingService.finalizeBooking(id);
        return ResponseEntity.ok(bookingMapper.toMap(b));
    }

    /* ======================================================
       USER – VIEW MY BOOKINGS
       ====================================================== */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ResponseEntity<?> getUserBookings(Authentication authentication) {
        List<Booking> bookings = bookingService.getUserBookings(authentication.getName());
        return ResponseEntity.ok(bookings.stream().map(bookingMapper::toMap).toList());
    }

    /* ======================================================
       PROVIDER – VIEW BOOKINGS
       ====================================================== */
    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVIDER')")
    @Transactional
    public List<BookingResponse> getProviderBookings(Authentication authentication) {
        List<Booking> bookings = bookingService.getProviderBookings(authentication.getName());

        return bookings.stream().map(booking -> {
            List<ProviderWorkLog> logs = workLogRepository.findByBookingId(booking.getId());

            int totalDays = 0, chargeableDays = 0, holidays = 0;
            for (ProviderWorkLog log : logs) {
                WorkStatus ws = log.getStatus();
                if (ws == WorkStatus.REJECTED) continue;
                totalDays++;
                if (ws == WorkStatus.PRESENT || ws == WorkStatus.CONFIRMED_PRESENT) chargeableDays++;
                else if (ws == WorkStatus.LEAVE) holidays++;
            }

            BookingStatus bookingStatus = booking.getStatus();
            boolean phoneVisible = PHONE_VISIBLE_STATUSES.contains(bookingStatus)
                    && booking.getUser() != null;

            Double totalAmount = booking.getTotalPrice();

            return BookingResponse.builder()
                    .bookingId(booking.getId())
                    .status(bookingStatus)
                    .customerName(booking.getUser() != null ? booking.getUser().getName() : "N/A")
                    .customerPhone(phoneVisible ? booking.getUser().getPhone() : null)
                    .serviceAddress(booking.getUser() != null ? booking.getUser().getAddress() : null)
                    .customerNote(booking.getCustomerNote())
                    .terminationReason(bookingStatus == BookingStatus.TERMINATED
                            ? booking.getTerminationReason() : null)
                    .serviceDate(booking.getAvailability() != null
                            ? booking.getAvailability().getDate().toString() : "-")
                    .services(new java.util.ArrayList<>(booking.getServices()))
                    .startTime(booking.getBookingStartTime() != null
                            ? booking.getBookingStartTime().toString() : "-")
                    .endTime(booking.getBookingEndTime() != null
                            ? booking.getBookingEndTime().toString() : "Ongoing")
                    .paymentStatus(booking.getPaymentStatus() != null
                            ? booking.getPaymentStatus().name() : "PENDING")
                    .totalAmount(totalAmount)
                    .totalDays(totalDays)
                    .chargeableDays(chargeableDays)
                    .holidays(holidays)
                    .build();
        }).toList();
    }

    /* ======================================================
       ADMIN – VIEW ALL BOOKINGS
       ====================================================== */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings.stream().map(bookingMapper::toMap).toList());
    }

    /* ======================================================
       USER – BOOKINGS WAITING FOR PAYMENT
       ====================================================== */
    @GetMapping("/user/payment-required")
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ResponseEntity<?> getPaymentRequiredBookings(Authentication auth) {
        List<Booking> bookings = bookingRepository.findByUser_EmailAndPaymentStatus(
                auth.getName(), PaymentStatus.PAYMENT_REQUIRED);
        return ResponseEntity.ok(bookings.stream().map(bookingMapper::toMap).toList());
    }

    /* ======================================================
       USER – PRICE PREVIEW
       ====================================================== */
    @PostMapping("/preview")
    @PreAuthorize("hasRole('USER')")
    public BookingPricePreviewResponse previewBooking(
            @RequestBody BookingPreviewRequestDTO request) {
        return bookingService.previewBookingPrice(request);
    }

    /* ======================================================
       USER – CREATE BOOKING
       ====================================================== */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createBooking(@RequestBody BookingPreviewRequestDTO request,
                                           Authentication authentication) {
        Booking booking = bookingService.createBooking(request, authentication.getName());
        return ResponseEntity.ok(Map.of(
                "id",                  booking.getId(),
                "totalPrice",          booking.getTotalPrice(),
                "walletEligible",      booking.getWalletEligible() != null ? booking.getWalletEligible() : 0.0,
                "walletConsentStatus", booking.getWalletConsentStatus() != null
                        ? booking.getWalletConsentStatus().name() : "PENDING",
                "paymentStatus",       booking.getPaymentStatus() != null
                        ? booking.getPaymentStatus().name() : "PENDING"
        ));
    }

    /* ======================================================
       USER – SUBMIT WALLET CONSENT
       ====================================================== */
    @PostMapping("/{id}/wallet-consent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> submitWalletConsent(
            @PathVariable Long id,
            @RequestParam boolean useWallet,
            Authentication authentication) {
        Booking booking = bookingService.submitWalletConsent(id, useWallet, authentication.getName());
        return ResponseEntity.ok(Map.of(
                "bookingId",           booking.getId(),
                "walletUsed",          booking.getWalletUsed() != null ? booking.getWalletUsed() : 0.0,
                "finalPayable",        booking.getFinalPayableAmount() != null
                        ? booking.getFinalPayableAmount() : booking.getTotalPrice(),
                "walletConsentStatus", booking.getWalletConsentStatus() != null
                        ? booking.getWalletConsentStatus().name() : "PENDING",
                "paymentStatus",       booking.getPaymentStatus() != null
                        ? booking.getPaymentStatus().name() : "PENDING"
        ));
    }

    /* ======================================================
       USER – PROVIDER OPTIONS
       ====================================================== */
    @PostMapping("/provider-options")
    @PreAuthorize("hasRole('USER')")
    public List<ProviderOptionDTO> getProviderOptions(
            @RequestBody BookingPreviewRequestDTO request) {
        return bookingService.getProviderOptions(request);
    }

    /* ======================================================
       GET SINGLE BOOKING BY ID
       ====================================================== */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','PROVIDER','ADMIN')")
    @Transactional
    public ResponseEntity<?> getBookingById(@PathVariable Long id,
                                            Authentication authentication) {
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
        return ResponseEntity.ok(
                bookingMapper.toMap(booking, totalDays, chargeableDays, absent, leave, rated)
        );
    }
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ResponseEntity<?> cancelBooking(@PathVariable Long id, Authentication authentication) {
        Booking b = bookingService.cancelBooking(id, authentication.getName());
        return ResponseEntity.ok(bookingMapper.toMap(b));
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