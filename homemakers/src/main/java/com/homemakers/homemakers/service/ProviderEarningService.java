package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import org.springframework.stereotype.Service;

@Service
public class ProviderEarningService {

    private final ProviderEarningRepository earningRepository;
    private final BookingRepository bookingRepository;

    public ProviderEarningService(
            ProviderEarningRepository earningRepository,
            BookingRepository bookingRepository
    ) {
        this.earningRepository = earningRepository;
        this.bookingRepository = bookingRepository;
    }

    public ProviderEarning createEarning(
            Provider provider,
            double amount,
            Long bookingId
    ) {
        // 🔑 FETCH BOOKING FIRST
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        ProviderEarning earning = new ProviderEarning();
        earning.setProvider(provider);
        earning.setBooking(booking);                 // ✅ NOW VALID
        earning.setAmount(amount);
        earning.setStatus(EarningStatus.PENDING);    // ✅ CORRECT ENUM

        return earningRepository.save(earning);
    }
}
