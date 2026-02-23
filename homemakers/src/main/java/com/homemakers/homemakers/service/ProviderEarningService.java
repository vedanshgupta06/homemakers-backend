package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderLeaveLedgerRepository;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class ProviderEarningService {

    private final ProviderEarningRepository earningRepository;
    private final ProviderWorkLogRepository workLogRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;

    public ProviderEarningService(
            ProviderEarningRepository earningRepository,
            ProviderWorkLogRepository workLogRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository
    ) {
        this.earningRepository = earningRepository;
        this.workLogRepository = workLogRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
    }

    /**
     * Generates weekly earnings including PAID leaves.
     */
    @Transactional
    public void generateWeeklyEarnings(LocalDate weekStart, LocalDate weekEnd) {

        List<ProviderWorkLog> logs =
                workLogRepository.findByWorkDateBetween(weekStart, weekEnd);

        // Group attendance per provider + booking
        Map<String, Integer> presentCountMap = new HashMap<>();
        Map<String, Provider> providerMap = new HashMap<>();
        Map<String, Booking> bookingMap = new HashMap<>();

        for (ProviderWorkLog log : logs) {

            if (log.getStatus() != WorkStatus.AUTO_PRESENT &&
                    log.getStatus() != WorkStatus.PRESENT) {
                continue;
            }

            String key = log.getProvider().getId() + "-" + log.getBooking().getId();

            presentCountMap.put(
                    key,
                    presentCountMap.getOrDefault(key, 0) + 1
            );

            providerMap.put(key, log.getProvider());
            bookingMap.put(key, log.getBooking());
        }

        int weekNo = weekStart.get(
                WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        );

        for (String key : presentCountMap.keySet()) {

            Provider provider = providerMap.get(key);
            Booking booking = bookingMap.get(key);

            // Prevent duplicate weekly earning
            if (earningRepository.existsByProviderAndBookingAndWeekNo(
                    provider, booking, weekNo)) {
                continue;
            }

            int presentDays = presentCountMap.getOrDefault(key, 0);

            // Count PAID leaves in this week
            long paidLeavesThisWeek =
                    leaveLedgerRepository
                            .countByProviderAndBookingAndLeaveTypeAndLeaveDateBetween(
                                    provider,
                                    booking,
                                    LeaveType.PAID,
                                    weekStart,
                                    weekEnd
                            );

            double dailyRate = booking.getTotalPrice() / 30.0;

            double totalAmount =
                    dailyRate * (presentDays + paidLeavesThisWeek);

            if (totalAmount <= 0) continue;

            ProviderEarning earning = new ProviderEarning();
            earning.setProvider(provider);
            earning.setBooking(booking);
            earning.setWeekNo(weekNo);
            earning.setAmount(totalAmount);
            earning.setStatus(EarningStatus.AVAILABLE);

            earningRepository.save(earning);
        }
    }
}