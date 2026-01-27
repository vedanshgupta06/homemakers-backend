package com.homemakers.homemakers.service.payout;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayoutCalculationService {

    private final ProviderRepository providerRepository;
    private final ProviderEarningRepository earningRepository;
    private final ProviderDeductionRepository deductionRepository;
    private final ProviderPayoutRepository payoutRepository;

    public PayoutCalculationService(
            ProviderRepository providerRepository,
            ProviderEarningRepository earningRepository,
            ProviderDeductionRepository deductionRepository,
            ProviderPayoutRepository payoutRepository
    ) {
        this.providerRepository = providerRepository;
        this.earningRepository = earningRepository;
        this.deductionRepository = deductionRepository;
        this.payoutRepository = payoutRepository;
    }

    @Transactional
    public ProviderPayout calculateMonthlyPayout(
            Long providerId,
            String payoutMonth
    ) {

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        // ❌ Prevent recalculation
        payoutRepository.findByProviderAndPayoutMonth(provider, payoutMonth)
                .ifPresent(p -> {
                    throw new RuntimeException("Payout already calculated for this month");
                });

        // 1️⃣ Fetch all unpaid earnings
        List<ProviderEarning> earnings =
                earningRepository.findByProviderAndStatus(
                        provider,
                        EarningStatus.PENDING
                );

        if (earnings.isEmpty()) {
            throw new RuntimeException("No earnings available for payout");
        }

        // 2️⃣ Fetch applicable deductions
        List<ProviderDeduction> deductions =
                deductionRepository.findByProviderAndStateIn(
                        provider,
                        List.of(
                                DeductionState.PROPOSED,
                                DeductionState.APPROVED
                        )
                );

        // 3️⃣ Calculate totals
        double totalEarnings = earnings.stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        double totalDeductions = deductions.stream()
                .mapToDouble(ProviderDeduction::getAmount)
                .sum();

        double net = Math.max(0, totalEarnings - totalDeductions);

        // 4️⃣ Create payout snapshot
        ProviderPayout payout = new ProviderPayout();
        payout.setProvider(provider);
        payout.setPayoutMonth(payoutMonth);
        payout.setTotalEarnings(totalEarnings);
        payout.setTotalDeductions(totalDeductions);
        payout.setNetPayout(net);
        payout.setStatus(PayoutStatus.CALCULATED);

        ProviderPayout savedPayout = payoutRepository.save(payout);

        // 5️⃣ LOCK earnings
        for (ProviderEarning earning : earnings) {
            earning.setStatus(EarningStatus.PAID);

        }

        // 6️⃣ LOCK deductions
        for (ProviderDeduction deduction : deductions) {
            deduction.setState(DeductionState.APPLIED);
            deduction.setPayout(savedPayout);
        }

        return savedPayout;
    }
}
