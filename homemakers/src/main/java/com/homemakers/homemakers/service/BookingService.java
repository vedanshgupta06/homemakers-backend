package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.BookingPricePreviewRequest;
import com.homemakers.homemakers.dto.BookingPricePreviewResponse;
import com.homemakers.homemakers.dto.BookingRequest;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ServicePricingRepository pricingRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;

    public BookingService(
            BookingRepository bookingRepository,
            ProviderAvailabilityRepository availabilityRepository,
            UserRepository userRepository,
            ProviderRepository providerRepository,
            ServicePricingRepository pricingRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.pricingRepository = pricingRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
    }

    // =========================================================
    // CREATE BOOKING
    // =========================================================
    @Transactional
    public Booking createBooking(BookingRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProviderAvailability slot = availabilityRepository.findById(request.getAvailabilityId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.isActive()) {
            throw new RuntimeException("Slot already booked");
        }

        Provider provider = slot.getProvider();

        double totalMonthlyPrice = 0;
        boolean hasHourlyService = false;

        for (ServiceType service : request.getServices()) {

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(
                            provider,
                            service,
                            provider.getCity()
                    )
                    .orElseThrow(() ->
                            new RuntimeException("Pricing not set for " + service)
                    );

            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                hasHourlyService = true;

                if (request.getHoursPerDay() == null || request.getHoursPerDay() <= 0) {
                    throw new RuntimeException("Hours per day required");
                }

                totalMonthlyPrice += pricing.getPricePerHour() * request.getHoursPerDay();
            } else {
                totalMonthlyPrice += pricing.getMonthlyRate();
            }
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setProvider(provider);
        booking.setAvailability(slot);
        booking.setServices(new HashSet<>(request.getServices()));
        booking.setTotalPrice(totalMonthlyPrice);
        booking.setHoursPerDay(hasHourlyService ? request.getHoursPerDay() : null);
        booking.setStatus(BookingStatus.PENDING);

        slot.setActive(false);
        availabilityRepository.save(slot);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // ACCEPT BOOKING
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

        // Work starts next day
        booking.markWorkStarted(LocalDate.now().plusDays(1));

        return bookingRepository.save(booking);
    }

    // =========================================================
    // PROVIDER DONE
    // =========================================================
    @Transactional
    public Booking markServiceDone(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.SERVICE_IN_PROGRESS &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Invalid booking state");
        }

        booking.setStatus(BookingStatus.SERVICE_DONE);
        booking.markWorkEnded(LocalDate.now());

        return bookingRepository.save(booking);
    }

    // =========================================================
    // SYSTEM COMPLETE
    // =========================================================
    @Transactional
    public Booking completeBookingBySystem(Long bookingId) {

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.SERVICE_DONE) {
            throw new RuntimeException("Booking not ready");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDate.now().atStartOfDay());

        return bookingRepository.save(booking);
    }

    // =========================================================
    // REJECT
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
    // FETCH USER BOOKINGS
    // =========================================================
    public List<Booking> getUserBookings(String email) {
        return bookingRepository.findByUser_Email(email);
    }

    // =========================================================
    // FETCH PROVIDER BOOKINGS (🔥 DYNAMIC CALCULATION)
    // =========================================================
    public List<Booking> getProviderBookings(String providerEmail) {

        Provider provider = providerRepository
                .findByUser_Email(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        List<Booking> bookings = bookingRepository.findByProvider(provider);

        for (Booking booking : bookings) {

            if (booking.getWorkStartDate() != null &&
                    booking.getStatus() == BookingStatus.SERVICE_IN_PROGRESS) {

                int totalDays = (int) ChronoUnit.DAYS.between(
                        booking.getWorkStartDate(),
                        LocalDate.now()
                );

                booking.setTotalDays(totalDays);

                int paidLeaves =
                        leaveLedgerRepository.countByBooking_IdAndLeaveType(
                                booking.getId(),
                                LeaveType.PAID
                        );

                int unpaidLeaves =
                        leaveLedgerRepository.countByBooking_IdAndLeaveType(
                                booking.getId(),
                                LeaveType.UNPAID
                        );

                int allowedPaidLeaves = 3;

                int extraUnpaidFromPaid =
                        Math.max(0, paidLeaves - allowedPaidLeaves);

                int finalUnpaidLeaves =
                        unpaidLeaves + extraUnpaidFromPaid;

                booking.setHolidays(Math.min(paidLeaves, allowedPaidLeaves));
                booking.setChargeableDays(totalDays - finalUnpaidLeaves);
            }
        }

        return bookings;
    }

    // =========================================================
    // PRICE PREVIEW
    // =========================================================
    public BookingPricePreviewResponse previewBookingPrice(
            BookingPricePreviewRequest request
    ) {
        Provider provider = providerRepository
                .findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        double total = 0;
        Map<String, Double> breakdown = new HashMap<>();

        for (ServiceType service : request.getServices()) {

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(
                            provider,
                            service,
                            provider.getCity()
                    )
                    .orElseThrow(() ->
                            new RuntimeException("Pricing not found")
                    );

            double price = pricing.getPricingType() == PricingType.HOURLY_MONTHLY
                    ? pricing.getPricePerHour() * request.getHoursPerDay()
                    : pricing.getMonthlyRate();

            breakdown.put(service.name(), price);
            total += price;
        }

        return new BookingPricePreviewResponse(total, breakdown);
    }
}