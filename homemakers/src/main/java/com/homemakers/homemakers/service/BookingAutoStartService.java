package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingAutoStartService {

    private final BookingRepository bookingRepository;

    public BookingAutoStartService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // ⏰ Runs every day at 6 AM
    @Scheduled(cron = "0 0 6 * * *")
    public void autoStartBookings() {

        List<Booking> bookings = bookingRepository
                .findByStatusAndPaymentStatus(
                        BookingStatus.CONFIRMED,
                        PaymentStatus.PAID
                );

        LocalDate today = LocalDate.now();

        for (Booking booking : bookings) {

            LocalDate startDate = booking.getAvailability().getDate();

            if (startDate.equals(today)) {

                booking.markWorkStarted(today);
                booking.setStatus(BookingStatus.SERVICE_IN_PROGRESS);

                bookingRepository.save(booking);

                System.out.println("✅ Auto-started booking: " + booking.getId());
            }
        }
    }
}
