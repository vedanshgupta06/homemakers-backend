package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProviderPayoutService {

    private final ProviderEarningRepository earningRepository;
    private final ProviderPayoutRepository payoutRepository;

    public ProviderPayoutService(
            ProviderEarningRepository earningRepository,
            ProviderPayoutRepository payoutRepository
    ) {
        this.earningRepository = earningRepository;
        this.payoutRepository = payoutRepository;
    }

    @Transactional
    public ProviderPayout requestPayout(Provider provider) {

        // 🔒 LOCK AVAILABLE EARNINGS
        List<ProviderEarning> available =
                earningRepository.findAvailableForUpdate(provider);

        if (available.isEmpty()) {
            throw new RuntimeException("No earnings available for withdrawal");
        }

        double total = available.stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        // 🔐 Prevent duplicate INITIATED payout
        boolean alreadyInitiated =
                payoutRepository.existsByProviderAndStatus(
                        provider,
                        PayoutStatus.INITIATED
                );

        if (alreadyInitiated) {
            throw new RuntimeException("Payout already initiated");
        }

        ProviderPayout payout = new ProviderPayout();
        payout.setProvider(provider);
        payout.setAmount(total);
        payout.setStatus(PayoutStatus.INITIATED);

        payoutRepository.save(payout);

        // 🔁 Move earnings → REQUESTED and link payout
        for (ProviderEarning earning : available) {
            earning.setStatus(EarningStatus.REQUESTED);
            earning.setPayout(payout); // VERY IMPORTANT
        }

        earningRepository.saveAll(available);

        return payout;
    }

}
