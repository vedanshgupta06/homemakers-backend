package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.*;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.util.ServiceDurationUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ServicePricingRepository pricingRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;
    private final BookingCalculationService bookingCalculationService;

    public BookingService(
            BookingRepository bookingRepository,
            ProviderAvailabilityRepository availabilityRepository,
            UserRepository userRepository,
            ProviderRepository providerRepository,
            ServicePricingRepository pricingRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository,
            BookingCalculationService bookingCalculationService
    ) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.pricingRepository = pricingRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
        this.bookingCalculationService = bookingCalculationService;
    }

    // =========================================================
    // CREATE BOOKING
    // =========================================================

    @Transactional
    public Booking createBooking(BookingPreviewRequestDTO request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProviderAvailability slot =
                availabilityRepository.findWithLockById(request.getAvailabilityId())
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.isActive()) {
            throw new RuntimeException("Slot already booked");
        }

        Provider provider = slot.getProvider();

        int requiredMinutes = 0;
        double totalMonthlyPrice = 0;

        Set<ServiceType> services = new HashSet<>();

        for (BookingServiceRequestDTO s : request.getServices()) {

            ServiceType type = s.getServiceType();
            services.add(type);

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(
                            provider,
                            type,
                            provider.getCity()
                    )
                    .orElseThrow(() ->
                            new RuntimeException("Pricing not set for " + type)
                    );

            Integer hours = s.getHours();

            // PRICE CALCULATION
            double price = bookingCalculationService.calculatePrice(
                    pricing,
                    type,
                    request.getMembers(),
                    request.getHouseSize(),
                    hours
            );

            totalMonthlyPrice += price;

            // TIME CALCULATION
            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                requiredMinutes += hours * 60;
            } else {
                requiredMinutes += ServiceDurationUtil.getMinutes(type);
            }
        }

        LocalTime requestStart = slot.getStartTime();
        LocalTime requestEnd = requestStart.plusMinutes(requiredMinutes);

        if (requestEnd.isAfter(slot.getEndTime())) {
            throw new RuntimeException("Requested time outside provider slot");
        }

        Booking booking = new Booking();

        booking.setUser(user);
        booking.setProvider(provider);
        booking.setAvailability(slot);
        booking.setServices(services);
        booking.setTotalPrice(totalMonthlyPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingStartTime(requestStart);
        booking.setBookingEndTime(requestEnd);

        bookingRepository.save(booking);

        // SLOT SPLITTING

        int MIN_SLOT_MINUTES = 20;

        long afterMinutes =
                Duration.between(requestEnd, slot.getEndTime()).toMinutes();

        if (afterMinutes >= MIN_SLOT_MINUTES) {

            ProviderAvailability afterSlot = new ProviderAvailability();

            afterSlot.setProvider(provider);
            afterSlot.setDate(slot.getDate());
            afterSlot.setStartTime(requestEnd);
            afterSlot.setEndTime(slot.getEndTime());
            afterSlot.setActive(true);

            availabilityRepository.save(afterSlot);
        }

        slot.setActive(false);
        availabilityRepository.save(slot);

        return booking;
    }

    // =========================================================
    // PROVIDER ACCEPT BOOKING
    // =========================================================

    @Transactional
    public Booking acceptBooking(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings allowed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAYMENT_REQUIRED);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // PROVIDER START WORK
    // =========================================================

    @Transactional
    public Booking startWork(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Payment not completed");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Booking not confirmed");
        }

        booking.markWorkStarted(LocalDate.now());
        booking.setStatus(BookingStatus.SERVICE_IN_PROGRESS);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // PROVIDER END WORK
    // =========================================================

//    @Transactional
//    public Booking endWork(Long bookingId, String providerEmail) {
//
//        Booking booking = bookingRepository
//                .findByIdAndProvider_User_Email(bookingId, providerEmail)
//                .orElseThrow(() -> new RuntimeException("Booking not found"));
//
//        if (booking.getStatus() != BookingStatus.SERVICE_IN_PROGRESS) {
//            throw new IllegalStateException("Work not in progress");
//        }
//
//        booking.markWorkEnded(LocalDate.now());
//        booking.setStatus(BookingStatus.SERVICE_DONE);
//
//        return bookingRepository.save(booking);
//    }

    // =========================================================
    // PROVIDER REJECT BOOKING
    // =========================================================

    @Transactional
    public Booking rejectBooking(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings allowed");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        ProviderAvailability availability = booking.getAvailability();
        availability.setActive(true);

        availabilityRepository.save(availability);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // ADMIN STOP SERVICE
    // =========================================================

//    @Transactional
//    public Booking stopWorkEarly(Long bookingId) {
//
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new RuntimeException("Booking not found"));
//
//        booking.markWorkEnded(LocalDate.now());
//        booking.setStatus(BookingStatus.SERVICE_STOPPED_BY_ADMIN);
//
//        return bookingRepository.save(booking);
//    }

    // =========================================================
    // ADMIN COMPLETE BOOKING
    // =========================================================

    @Transactional
    public Booking finalizeBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    // =========================================================
    // USER BOOKINGS
    // =========================================================

    public List<Booking> getUserBookings(String email) {
        return bookingRepository.findByUser_Email(email);
    }

    // =========================================================
    // PROVIDER BOOKINGS
    // =========================================================

    public List<Booking> getProviderBookings(String providerEmail) {

        Provider provider = providerRepository
                .findByUser_Email(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return bookingRepository.findByProvider(provider);
    }

    // =========================================================
    // ADMIN ALL BOOKINGS
    // =========================================================

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    // =========================================================
    // PRICE PREVIEW
    // =========================================================

    public BookingPricePreviewResponse previewBookingPrice(
            BookingPreviewRequestDTO request
    ) {

        Provider provider = providerRepository
                .findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        double total = 0;
        Map<String, Double> breakdown = new HashMap<>();

        ProviderAvailability slot =
                availabilityRepository.findById(request.getAvailabilityId())
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

        for (BookingServiceRequestDTO s : request.getServices()) {

            ServiceType service = s.getServiceType();
            Integer hours = s.getHours();

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(
                            provider,
                            service,
                            provider.getCity()
                    )
                    .orElseThrow(() -> new RuntimeException("Pricing not found"));

            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                if (hours == null || hours <= 0) {
                    throw new RuntimeException("Hours per day required for hourly services");
                }
            }
            System.out.println("SERVICE = " + service +
                    " HOURS = " + hours +
                    " TYPE = " + pricing.getPricingType());
            double price = bookingCalculationService.calculatePrice(
                    pricing,
                    service,
                    request.getMembers(),
                    request.getHouseSize(),
                    hours
            );

            breakdown.put(service.name(), price);
            total += price;
        }
        return new BookingPricePreviewResponse(
                total,
                breakdown,
                provider.getUser().getName(),
                slot.getStartTime().toString(),
                slot.getEndTime().toString(),
                request.getHouseSize(),
                request.getMembers()
        );
    }
    public void autoCompleteBooking(Booking booking) {

        if (booking.getStatus() == BookingStatus.SERVICE_IN_PROGRESS) {

            if (booking.getWorkStartDate() != null &&
                    booking.getWorkStartDate().plusDays(30).isBefore(java.time.LocalDate.now())) {

                booking.setStatus(BookingStatus.COMPLETED);
                booking.setWorkEndDate(booking.getWorkStartDate().plusDays(30));
            }
        }
    }
}