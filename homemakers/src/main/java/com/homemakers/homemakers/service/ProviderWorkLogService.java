package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProviderWorkLogService {

    @Autowired
    private ProviderWorkLogRepository workLogRepository;

    @Autowired
    private ProviderEarningService earningService;

    @Transactional
    public ProviderWorkLog saveWorkLog(ProviderWorkLog workLog) {

        if (workLog == null) {
            throw new IllegalArgumentException("WorkLog cannot be null");
        }

        return workLogRepository.save(workLog);
    }

    // 🔵 Provider marks present
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
    }

    // 🔵 Customer confirms attendance
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

        // 🔥 Earning only after confirmation
        earningService.generateDailyEarning(log);
    }

    // 🔵 Customer rejects
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
    }
}