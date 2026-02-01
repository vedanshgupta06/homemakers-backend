package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderLeaveLedgerRepository;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProviderLeaveSettlementService {

    private final ProviderWorkLogRepository workLogRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;
    private final ProviderAutoDeductionService autoDeductionService;

    public ProviderLeaveSettlementService(
            ProviderWorkLogRepository workLogRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository,
            ProviderAutoDeductionService autoDeductionService
    ) {
        this.workLogRepository = workLogRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
        this.autoDeductionService = autoDeductionService;
    }

    @Transactional
    public void settleLeaves(Provider provider, Booking booking) {

        // 1️⃣ Count worked days
        long workedDays =
                workLogRepository.countByProviderAndBookingAndStatus(
                        provider, booking, WorkStatus.WORKED
                );

        int earnedPaidLeaves = (int) Math.min(workedDays / 10, 3);

        long paidLeavesUsed =
                leaveLedgerRepository.countByProviderAndBookingAndLeaveType(
                        provider, booking, LeaveType.PAID
                );

        // 2️⃣ Fetch all leave work logs
        var leaveLogs =
                workLogRepository.findByProviderAndBookingAndStatus(
                        provider, booking, WorkStatus.LEAVE
                );

        for (ProviderWorkLog log : leaveLogs) {

            // Skip if already settled
            boolean exists =
                    leaveLedgerRepository.existsByProviderAndBookingAndLeaveDate(
                            provider,
                            booking,
                            log.getWorkDate()
                    );


            if (exists) continue;

            LeaveType leaveType =
                    paidLeavesUsed < earnedPaidLeaves
                            ? LeaveType.PAID
                            : LeaveType.UNPAID;

            leaveLedgerRepository.save(
                    new ProviderLeaveLedger(
                            provider,
                            booking,
                            log.getWorkDate(),
                            leaveType,
                            "Auto-settled from work log"
                    )
            );

            if (leaveType == LeaveType.PAID) {
                paidLeavesUsed++;
            }
        }
        // 3️⃣ Apply auto deductions for unpaid leaves
        autoDeductionService.applyUnpaidLeaveDeductions(provider, booking);

    }
}

