package com.homemakers.homemakers.service.attendance;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.service.ProviderLeaveSettlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceGenerationService {

    private final BookingRepository bookingRepository;
    private final ProviderWorkLogRepository workLogRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;
    private final ProviderLeaveSettlementService leaveSettlementService;

    public AttendanceGenerationService(
            BookingRepository bookingRepository,
            ProviderWorkLogRepository workLogRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository,
            ProviderLeaveSettlementService leaveSettlementService
    ) {
        this.bookingRepository = bookingRepository;
        this.workLogRepository = workLogRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
        this.leaveSettlementService = leaveSettlementService;
    }

    @Transactional
    public void generateDailyAttendance(LocalDate date) {

        List<Booking> activeBookings =
                bookingRepository.findActiveForAttendance(
                        List.of(
                                BookingStatus.CONFIRMED,
                                BookingStatus.SERVICE_IN_PROGRESS
                        ),
                        date
                );

        for (Booking booking : activeBookings) {

            Provider provider = booking.getProvider();

            // Prevent duplicate log
            if (workLogRepository.existsByProviderAndBookingAndWorkDate(
                    provider, booking, date)) {
                continue;
            }

            WorkStatus status;

            // Check approved leave for this booking
            if (leaveLedgerRepository.existsApprovedLeave(provider, booking, date)) {
                status = WorkStatus.LEAVE;
            } else {
                status = WorkStatus.PENDING;
            }

            ProviderWorkLog log = new ProviderWorkLog();
            log.setProvider(provider);
            log.setBooking(booking);
            log.setWorkDate(date);
            log.setStatus(status);

            workLogRepository.save(log);

            // settle leave balance
            leaveSettlementService.settleLeaves(provider, booking);
        }
    }
}