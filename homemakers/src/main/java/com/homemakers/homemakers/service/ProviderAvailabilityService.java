package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.AvailabilityRequest;
import com.homemakers.homemakers.dto.AvailabilityResponse;
import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderAvailability;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderAvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProviderAvailabilityService {

    private final ProviderAvailabilityRepository availabilityRepo;
    private final BookingRepository bookingRepo;

    public ProviderAvailabilityService(ProviderAvailabilityRepository availabilityRepo,
                                       BookingRepository bookingRepo) {
        this.availabilityRepo = availabilityRepo;
        this.bookingRepo = bookingRepo;
    }

    public void addAvailability(Provider provider, AvailabilityRequest request) {

        if (request.getDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot add availability for past dates");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Check 1: same-day time overlap with any existing slot (active or booked)
        boolean sameDayOverlap = availabilityRepo
                .existsByProviderAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                        provider,
                        request.getDate(),
                        request.getEndTime(),
                        request.getStartTime()
                );
        if (sameDayOverlap) throw new RuntimeException("Availability overlaps with existing slot");

        // Check 2: Monthly booking range check.
        //
        // A booking is a MONTHLY service — the provider works the same hours EVERY DAY
        // for ~30 days. So we must check each active booking individually and compute
        // its estimated end date from its OWN availability.date (not from request.getDate()).
        //
        // Example: booking started May 1, 12:00–1:00 PM → occupies every day until May 31.
        // Adding a slot for May 15, 12:30–2:00 PM must be blocked.
        //
        // We loop through all active bookings for this provider and check each one.
        List<Booking> activeBookings = bookingRepo.findActiveBookingsForProvider(provider.getId());

        for (Booking booking : activeBookings) {

            // Determine the booking's work range
            LocalDate rangeStart;
            LocalDate rangeEnd;

            if (booking.getWorkStartDate() != null) {
                // Work has started — use actual dates
                rangeStart = booking.getWorkStartDate();
                rangeEnd   = booking.getWorkEndDate() != null
                        ? booking.getWorkEndDate()
                        : booking.getWorkStartDate().plusDays(30);
            } else {
                // CONFIRMED but not started — estimate from slot date
                LocalDate slotDate = booking.getAvailability() != null
                        ? booking.getAvailability().getDate()
                        : null;
                if (slotDate == null) continue;
                rangeStart = slotDate;
                rangeEnd   = slotDate.plusDays(30);
            }

            // Is the new slot's date within this booking's work range?
            boolean dateInRange = !request.getDate().isBefore(rangeStart)
                    && !request.getDate().isAfter(rangeEnd);

            if (!dateInRange) continue;

            // Does the new slot's time overlap with this booking's daily hours?
            LocalTime bStart = booking.getBookingStartTime();
            LocalTime bEnd   = booking.getBookingEndTime();

            if (bStart == null || bEnd == null) continue;

            boolean timesOverlap = request.getStartTime().isBefore(bEnd)
                    && request.getEndTime().isAfter(bStart);

            if (timesOverlap) {
                throw new RuntimeException(
                        "Date falls within an ongoing booking period " +
                                "(" + rangeStart + " → " + rangeEnd + " · " +
                                bStart + "–" + bEnd + ")"
                );
            }
        }

        ProviderAvailability availability = new ProviderAvailability();
        availability.setProvider(provider);
        availability.setDate(request.getDate());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setActive(true);
        availabilityRepo.save(availability);
    }

    // Returns AvailabilityResponse with booking work range attached to booked slots
    public List<AvailabilityResponse> getAvailabilityForProvider(Long providerId) {

        List<ProviderAvailability> slots = availabilityRepo.findByProvider_Id(providerId);

        return slots.stream().map(slot -> {

            LocalDate bookingWorkStart = null;
            LocalDate bookingWorkEnd = null;
            String bookingCustomerName = null;

            if (!slot.isActive()) {
                Optional<Booking> booking = bookingRepo.findByAvailability_Id(slot.getId());
                if (booking.isPresent()) {
                    Booking b = booking.get();
                    bookingWorkStart = b.getWorkStartDate();
                    bookingWorkEnd   = b.getWorkEndDate();
                    bookingCustomerName = b.getUser() != null ? b.getUser().getName() : null;
                }
            }

            return new AvailabilityResponse(
                    slot.getId(),
                    slot.getDate(),
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.isActive(),
                    slot.getDurationMinutes(),
                    bookingWorkStart,
                    bookingWorkEnd,
                    bookingCustomerName
            );

        }).toList();
    }
}