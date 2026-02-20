package com.homemakers.homemakers.scheduler;

import com.homemakers.homemakers.service.ProviderEarningService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class WeeklyEarningScheduler {

    private final ProviderEarningService earningService;

    public WeeklyEarningScheduler(
            ProviderEarningService earningService
    ) {
        this.earningService = earningService;
    }

    @Scheduled(cron = "0 0 1 ? * MON") // every Monday 1 AM
    public void generateWeeklyEarnings() {

        LocalDate today = LocalDate.now();
        LocalDate lastWeekStart = today.minusWeeks(1)
                .with(DayOfWeek.MONDAY);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        earningService.generateWeeklyEarnings(
                lastWeekStart,
                lastWeekEnd
        );
    }
}

