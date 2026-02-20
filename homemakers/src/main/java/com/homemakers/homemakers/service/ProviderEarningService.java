package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
public class ProviderEarningService {

    private final ProviderEarningRepository earningRepository;
    private final ProviderWorkLogRepository workLogRepository;

    public ProviderEarningService(
            ProviderEarningRepository earningRepository,
            ProviderWorkLogRepository workLogRepository
    ) {
        this.earningRepository = earningRepository;
        this.workLogRepository = workLogRepository;
    }

    /**
     * Generates weekly earnings based purely on attendance.
     */
    @Transactional
    public void generateWeeklyEarnings(LocalDate weekStart, LocalDate weekEnd) {

        List<ProviderWorkLog> logs =
                workLogRepository.findByWorkDateBetween(weekStart, weekEnd);

        for (ProviderWorkLog log : logs) {

            if (log.getStatus() != WorkStatus.AUTO_PRESENT &&
                    log.getStatus() != WorkStatus.PRESENT) {
                continue;
            }

            Booking booking = log.getBooking();
            Provider provider = log.getProvider();

            double dailyRate =
                    booking.getTotalPrice() / booking.getTotalDays();

            int weekNo = log.getWorkDate()
                    .get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());

            // Prevent duplicate weekly earning
            if (earningRepository.existsByProviderAndBookingAndWeekNo(
                    provider, booking, weekNo)) {
                continue;
            }

            ProviderEarning earning = new ProviderEarning();
            earning.setProvider(provider);
            earning.setBooking(booking);
            earning.setAmount(dailyRate);
            earning.setWeekNo(weekNo);
            earning.setStatus(EarningStatus.AVAILABLE);

            earningRepository.save(earning);
        }
    }
}
