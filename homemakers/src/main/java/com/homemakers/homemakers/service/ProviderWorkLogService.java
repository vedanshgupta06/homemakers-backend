package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ProviderWorkLogService {

    private final ProviderWorkLogRepository workLogRepository;
    private final ProviderEarningService earningService;
    private final ProviderNotificationService notificationService; // ✅ NEW

    public ProviderWorkLogService(
            ProviderWorkLogRepository workLogRepository,
            ProviderEarningService earningService,
            ProviderNotificationService notificationService // ✅ NEW
    ) {
        this.workLogRepository = workLogRepository;
        this.earningService = earningService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProviderWorkLog saveWorkLog(ProviderWorkLog workLog) {
        if (workLog == null) throw new IllegalArgumentException("WorkLog cannot be null");
        return workLogRepository.save(workLog);
    }

    @Transactional
    public void markPresent(Long workLogId, Provider provider) {

        ProviderWorkLog log = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new RuntimeException("Work log not found"));

        if (!log.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        if (log.getStatus() != WorkStatus.PENDING) {
            throw new RuntimeException("Invalid state transition");
        }

        log.setStatus(WorkStatus.PRESENT);
        workLogRepository.save(log);

        Booking booking = log.getBooking();
        LocalDate start = booking.getWorkStartDate();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (LocalDate date = start; !date.isAfter(yesterday); date = date.plusDays(1)) {
            LocalDate d = date;
            boolean alreadyLogged = workLogRepository
                    .existsByBookingAndProviderAndWorkDate(booking, provider, d);
            if (!alreadyLogged) {
                ProviderWorkLog absent = new ProviderWorkLog();
                absent.setBooking(booking);
                absent.setProvider(provider);
                absent.setWorkDate(d);
                absent.setStatus(WorkStatus.ABSENT);
                workLogRepository.save(absent);
            }
        }
    }

    @Transactional
    public void confirmAttendance(Long workLogId, User customer) {

        ProviderWorkLog log = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new RuntimeException("Work log not found"));

        Booking booking = log.getBooking();

        if (!booking.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (log.getStatus() != WorkStatus.PRESENT) {
            throw new RuntimeException("Invalid state transition");
        }

        log.setStatus(WorkStatus.CONFIRMED_PRESENT);
        earningService.generateDailyEarning(log);

        // ✅ Notify provider attendance confirmed
        notificationService.attendanceConfirmed(
                log.getProvider(),
                booking.getId(),
                log.getWorkDate().toString()
        );
    }

    @Transactional
    public void rejectAttendance(Long workLogId, User customer) {

        ProviderWorkLog log = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new RuntimeException("Work log not found"));

        Booking booking = log.getBooking();

        if (!booking.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (log.getStatus() != WorkStatus.PRESENT) {
            throw new RuntimeException("Invalid state transition");
        }

        log.setStatus(WorkStatus.REJECTED);
        workLogRepository.save(log);

        // ✅ Notify provider attendance rejected
        notificationService.attendanceRejected(
                log.getProvider(),
                booking.getId(),
                log.getWorkDate().toString()
        );
    }

    public void markLeave(Long logId, Provider provider) {
        ProviderWorkLog log = workLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Work log not found"));

        if (!log.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        log.setStatus(WorkStatus.LEAVE);
        workLogRepository.save(log);
    }
    @Transactional
    public void undoAttendance(Long workLogId, User customer) {
        ProviderWorkLog log = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new RuntimeException("Work log not found"));

        if (!log.getBooking().getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Only allow undo of CONFIRMED_PRESENT or REJECTED
        if (log.getStatus() != WorkStatus.CONFIRMED_PRESENT &&
                log.getStatus() != WorkStatus.REJECTED) {
            throw new RuntimeException("Cannot undo this status");
        }

        log.setStatus(WorkStatus.PRESENT); // revert to pending approval
        workLogRepository.save(log);
    }
}