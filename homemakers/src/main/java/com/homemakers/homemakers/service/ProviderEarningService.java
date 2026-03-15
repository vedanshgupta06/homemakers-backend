package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderLeaveLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ProviderEarningService {

    private final ProviderEarningRepository earningRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;
    private final BookingRepository bookingRepository;

    public ProviderEarningService(
            ProviderEarningRepository earningRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository,
            BookingRepository bookingRepository
    ) {
        this.earningRepository = earningRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public void generateDailyEarning(ProviderWorkLog log) {

        if (log == null || log.getStatus() == null) {
            return;
        }

        WorkStatus status = log.getStatus();

        // ✅ Only allow CONFIRMED_PRESENT or LEAVE
        if (status != WorkStatus.CONFIRMED_PRESENT &&
                status != WorkStatus.LEAVE) {
            return;
        }

        Provider provider = log.getProvider();
        Booking booking = log.getBooking();
        LocalDate workDate = log.getWorkDate();

        if (provider == null || booking == null || workDate == null) {
            return;
        }

        // 🔒 Stop service after 30 calendar days
        LocalDate serviceStart = booking.getCreatedAt().toLocalDate();
        LocalDate serviceEnd = serviceStart.plusDays(29);

        if (workDate.isAfter(serviceEnd)) {
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
            return;
        }

        // 🔒 Prevent duplicate earning for same date
        boolean exists =
                earningRepository.existsByProviderAndBookingAndWorkDate(
                        provider,
                        booking,
                        workDate
                );

        if (exists) {
            return;
        }

        double dailyRate = booking.getTotalPrice() / 30.0;
        double amount = 0;

        // ✅ Customer confirmed present
        if (status == WorkStatus.CONFIRMED_PRESENT) {
            amount = dailyRate;
        }

        // ✅ Leave (only if PAID)
        else if (status == WorkStatus.LEAVE) {

            boolean isPaidLeave =
                    leaveLedgerRepository.existsByProviderAndBookingAndLeaveDateAndLeaveType(
                            provider,
                            booking,
                            workDate,
                            LeaveType.PAID
                    );

            if (isPaidLeave) {
                amount = dailyRate;
            }
        }

        if (amount <= 0) {
            return;
        }

        ProviderEarning earning = new ProviderEarning();
        earning.setProvider(provider);
        earning.setBooking(booking);
        earning.setWorkDate(workDate);
        earning.setAmount(amount);
        earning.setStatus(EarningStatus.AVAILABLE);

        earningRepository.save(earning);
    }
}