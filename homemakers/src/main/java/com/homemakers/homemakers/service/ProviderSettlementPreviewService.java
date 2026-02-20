package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.SettlementPreview;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.stereotype.Service;
@Service
public class ProviderSettlementPreviewService {

    private final ProviderWorkLogRepository workLogRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;

    public ProviderSettlementPreviewService(
            ProviderWorkLogRepository workLogRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository
    ) {
        this.workLogRepository = workLogRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
    }

    public SettlementPreview calculatePreview(
            Provider provider,
            Booking booking
    ) {

        long workedDays =
                workLogRepository.countByProviderAndBookingAndStatusIn(
                        provider,
                        booking,
                        java.util.List.of(
                                WorkStatus.AUTO_PRESENT,
                                WorkStatus.PRESENT
                        )
                );

        long leaveDays =
                workLogRepository.countByProviderAndBookingAndStatus(
                        provider, booking, WorkStatus.LEAVE
                );

        int earnedPaidLeaves = (int) Math.min(workedDays / 10, 3);

        long paidLeavesUsed =
                leaveLedgerRepository.countByProviderAndBookingAndLeaveType(
                        provider, booking, LeaveType.PAID
                );

        long unpaidLeaves =
                leaveLedgerRepository.countByProviderAndBookingAndLeaveType(
                        provider, booking, LeaveType.UNPAID
                );

        double dailySalary =
                booking.getChargeableDays() == 0
                        ? 0
                        : booking.getTotalPrice() / booking.getChargeableDays();

        double potentialDeduction =
                unpaidLeaves * dailySalary;

        return new SettlementPreview(
                workedDays,
                leaveDays,
                earnedPaidLeaves,
                paidLeavesUsed,
                unpaidLeaves,
                dailySalary,
                potentialDeduction
        );
    }
}
