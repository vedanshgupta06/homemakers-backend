package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.BookingPricePreviewRequest;
import com.homemakers.homemakers.dto.BookingPricePreviewResponse;
import com.homemakers.homemakers.dto.BookingRequest;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ProviderEarningService providerEarningService;
    private final ServicePricingRepository pricingRepository;

    public BookingService(
            BookingRepository bookingRepository,
            ProviderAvailabilityRepository availabilityRepository,
            UserRepository userRepository,
            ProviderRepository providerRepository,
            ProviderEarningService providerEarningService,
            ServicePricingRepository pricingRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.providerEarningService = providerEarningService;
        this.pricingRepository = pricingRepository;
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
                    throw new RuntimeException("Hours per day required for hourly service");
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

        // Lock slot
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
            throw new RuntimeException("Only PENDING bookings can be accepted");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    // =========================================================
    // COMPLETE BOOKING (🔥 CREATES PROVIDER EARNING)
    // =========================================================
    @Transactional
    public Booking completeBooking(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only CONFIRMED bookings can be completed");
        }

        // 1️⃣ Mark booking completed
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        // 2️⃣ Create provider earning (ONCE per booking)
        providerEarningService.createEarning(
                booking.getProvider(),
                booking.getTotalPrice(),
                booking.getId()
        );

        return booking;
    }

    // =========================================================
    // REJECT BOOKING
    // =========================================================
    @Transactional
    public Booking rejectBooking(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);

        ProviderAvailability availability = booking.getAvailability();
        availability.setActive(true);
        availabilityRepository.save(availability);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // FETCH BOOKINGS
    // =========================================================
    public List<Booking> getUserBookings(String email) {
        return bookingRepository.findByUser_Email(email);
    }

    public List<Booking> getProviderBookings(String providerEmail) {

        Provider provider = providerRepository
                .findByUser_Email(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return bookingRepository.findByProvider(provider);
    }

    // =========================================================
    // PRICE PREVIEW
    // =========================================================
    @Transactional
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
                            new RuntimeException("Pricing not found for " + service)
                    );

            double price;

            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                price = pricing.getPricePerHour() * request.getHoursPerDay();
            } else {
                price = pricing.getMonthlyRate();
            }

            breakdown.put(service.name(), price);
            total += price;
        }

        return new BookingPricePreviewResponse(total, breakdown);
    }
}
