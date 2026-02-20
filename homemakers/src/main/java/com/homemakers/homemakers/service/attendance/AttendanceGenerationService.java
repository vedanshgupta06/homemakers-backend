package com.homemakers.homemakers.service.attendance;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceGenerationService {

    private final BookingRepository bookingRepository;
    private final ProviderWorkLogRepository workLogRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;

    public AttendanceGenerationService(
            BookingRepository bookingRepository,
            ProviderWorkLogRepository workLogRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.workLogRepository = workLogRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
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

            if (workLogRepository.existsByProviderAndBookingAndWorkDate(
                    provider, booking, date)) {
                continue;
            }

            WorkStatus status;

            if (leaveLedgerRepository.existsApprovedLeave(provider, date)) {
                status = WorkStatus.LEAVE;
            } else {
                status = WorkStatus.AUTO_PRESENT;
            }

            ProviderWorkLog log = new ProviderWorkLog();
            log.setProvider(provider);
            log.setBooking(booking);
            log.setWorkDate(date);
            log.setStatus(status);

            workLogRepository.save(log);
        }
    }
}
