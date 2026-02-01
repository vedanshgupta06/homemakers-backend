package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ProviderReplacementService {

    private final BookingRepository bookingRepository;
    private final ProviderWorkLogRepository workLogRepository;

    public ProviderReplacementService(
            BookingRepository bookingRepository,
            ProviderWorkLogRepository workLogRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.workLogRepository = workLogRepository;
    }

    @Transactional
    public void replaceProvider(
            Booking booking,
            Provider newProvider,
            LocalDate replacementDate
    ) {

        Provider oldProvider = booking.getProvider();

        // 1️⃣ Close old provider work logs
        workLogRepository.findByProviderAndBooking(oldProvider, booking)
                .stream()
                .filter(log -> !log.getWorkDate().isAfter(replacementDate))
                .forEach(log -> {
                    // no mutation needed, logs are already historical
                });

        // 2️⃣ Mark booking as partially completed
        booking.setStatus(BookingStatus.PARTIALLY_COMPLETED);

        // 3️⃣ Assign new provider
        booking.setProvider(newProvider);

        bookingRepository.save(booking);

        // 4️⃣ New provider starts fresh (no logs yet)
        // Work logs, leaves, payouts will begin from replacementDate
    }
}
