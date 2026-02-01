package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderDeductionRepository;
import com.homemakers.homemakers.repository.ProviderLeaveLedgerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProviderAutoDeductionService {

    private final ProviderLeaveLedgerRepository leaveLedgerRepository;
    private final ProviderDeductionRepository deductionRepository;

    public ProviderAutoDeductionService(
            ProviderLeaveLedgerRepository leaveLedgerRepository,
            ProviderDeductionRepository deductionRepository
    ) {
        this.leaveLedgerRepository = leaveLedgerRepository;
        this.deductionRepository = deductionRepository;
    }

    @Transactional
    public void applyUnpaidLeaveDeductions(Provider provider, Booking booking) {

        long unpaidLeaves =
                leaveLedgerRepository.countByProviderAndBookingAndLeaveType(
                        provider,
                        booking,
                        LeaveType.UNPAID
                );

        if (unpaidLeaves == 0) return;

        double dailySalary =
                booking.getTotalPrice() / booking.getChargeableDays();

        double deductionAmount =
                unpaidLeaves * dailySalary;

        ProviderDeduction deduction = new ProviderDeduction();
        deduction.setProvider(provider);
        deduction.setBooking(booking);
        deduction.setType(DeductionType.EXCESS_HOLIDAY);
        deduction.setAmount(deductionAmount);
        deduction.setReason("Auto deduction for unpaid leaves");
        deduction.setSourceType(DeductionSourceType.SYSTEM_ADJUSTMENT);
        deduction.setState(DeductionState.PROPOSED);

        deductionRepository.save(deduction);
    }
}
