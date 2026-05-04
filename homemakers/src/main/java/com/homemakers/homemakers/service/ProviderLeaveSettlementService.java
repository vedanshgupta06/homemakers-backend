package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderLeaveLedgerRepository;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.homemakers.homemakers.model.WorkStatus.CONFIRMED_PRESENT;
import static com.homemakers.homemakers.model.WorkStatus.PRESENT;

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

        // 1️⃣ Count worked days (both PRESENT and CONFIRMED_PRESENT)
        long workedDays = workLogRepository.countByProviderAndBookingAndStatusIn(
                provider,
                booking,
                List.of(CONFIRMED_PRESENT, PRESENT)
        );

        // 2️⃣ Paid leave entitlement based on worked days (range-based rule)
        // worked > 25 → 3 paid leaves allowed
        // worked > 15 → 2 paid leaves allowed
        // worked >  5 → 1 paid leave  allowed
        // otherwise   → 0
        int earnedPaidLeaves;
        if (workedDays > 23) {
            earnedPaidLeaves = 3;
        } else if (workedDays > 15) {
            earnedPaidLeaves = 2;
        } else if (workedDays > 5) {
            earnedPaidLeaves = 1;
        } else {
            earnedPaidLeaves = 0;
        }

        // 3️⃣ Count absent days — provider missed work without applying leave
        //    These are still eligible for paid leave coverage if entitlement remains
        long absentDays = workLogRepository.countByProviderAndBookingAndStatus(
                provider, booking, WorkStatus.ABSENT
        );

        // Total non-worked days (leave + absent) caps the paid leave credit
        // so we never give more paid leave than days actually missed
        var leaveLogs = workLogRepository.findByProviderAndBookingAndStatus(
                provider, booking, WorkStatus.LEAVE
        );
        long totalNonWorkedDays = leaveLogs.size() + absentDays;
        earnedPaidLeaves = (int) Math.min(earnedPaidLeaves, totalNonWorkedDays);

        // 4️⃣ How many paid leaves already recorded in ledger
        long paidLeavesUsed = leaveLedgerRepository.countByProviderAndBookingAndLeaveType(
                provider, booking, LeaveType.PAID
        );

        // 5️⃣ Settle explicit LEAVE logs first (provider applied for leave)
        for (ProviderWorkLog log : leaveLogs) {

            // Skip if already settled
            boolean exists = leaveLedgerRepository.existsByProviderAndBookingAndLeaveDate(
                    provider, booking, log.getWorkDate()
            );
            if (exists) continue;

            LeaveType leaveType = paidLeavesUsed < earnedPaidLeaves
                    ? LeaveType.PAID
                    : LeaveType.UNPAID;

            leaveLedgerRepository.save(new ProviderLeaveLedger(
                    provider,
                    booking,
                    log.getWorkDate(),
                    leaveType,
                    "Auto-settled from work log"
            ));

            if (leaveType == LeaveType.PAID) {
                paidLeavesUsed++;
            }
        }

        // 6️⃣ Settle ABSENT days using remaining paid leave entitlement
        //    Provider didn't apply for leave but is still entitled to paid coverage
        if (paidLeavesUsed < earnedPaidLeaves) {

            var absentLogs = workLogRepository.findByProviderAndBookingAndStatus(
                    provider, booking, WorkStatus.ABSENT
            );

            for (ProviderWorkLog log : absentLogs) {

                if (paidLeavesUsed >= earnedPaidLeaves) break;

                // Skip if already settled as paid leave
                boolean exists = leaveLedgerRepository.existsByProviderAndBookingAndLeaveDate(
                        provider, booking, log.getWorkDate()
                );
                if (exists) continue;

                // Cover this absent day with remaining paid leave entitlement
                leaveLedgerRepository.save(new ProviderLeaveLedger(
                        provider,
                        booking,
                        log.getWorkDate(),
                        LeaveType.PAID,
                        "Auto-settled: absent day covered by paid leave entitlement"
                ));

                paidLeavesUsed++;
            }
        }

        // 7️⃣ Apply auto deductions for unpaid leaves
        autoDeductionService.applyUnpaidLeaveDeductions(provider, booking);
    }
}