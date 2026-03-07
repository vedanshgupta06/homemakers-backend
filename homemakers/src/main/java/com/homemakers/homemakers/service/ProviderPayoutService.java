package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;
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

        // 🔒 Weekly withdrawal enforcement
        if (provider.getLastPayoutRequestedAt() != null) {

            LocalDateTime nextAllowedDate =
                    provider.getLastPayoutRequestedAt().plusDays(7);

            if (LocalDateTime.now().isBefore(nextAllowedDate)) {
                throw new RuntimeException(
                        "You can request payout only once every 7 days"
                );
            }
        }

        // 🔐 Prevent duplicate INITIATED payout
        boolean alreadyInitiated =
                payoutRepository.existsByProviderAndStatus(
                        provider,
                        PayoutStatus.INITIATED
                );

        if (alreadyInitiated) {
            throw new RuntimeException("Payout already initiated");
        }

        // 🔒 Lock available earnings
        List<ProviderEarning> available =
                earningRepository.findAvailableForUpdate(provider);

        if (available.isEmpty()) {
            throw new RuntimeException("No earnings available for withdrawal");
        }

        double total = available.stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        ProviderPayout payout = new ProviderPayout();
        payout.setProvider(provider);
        payout.setAmount(total);
        payout.setStatus(PayoutStatus.INITIATED);
        payout.setPayoutMonth(LocalDate.now().getMonth().name());
        payoutRepository.save(payout);

        // 🔁 Move earnings → REQUESTED
        for (ProviderEarning earning : available) {
            earning.setStatus(EarningStatus.REQUESTED);
            earning.setPayout(payout);
        }

        earningRepository.saveAll(available);

        // 🕒 Update last payout request timestamp
        provider.setLastPayoutRequestedAt(LocalDateTime.now());

        return payout;
    }
    public List<ProviderPayout> getPayoutHistory(Provider provider) {
        return payoutRepository.findByProviderOrderByCreatedAtDesc(provider);
    }

}
