package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.WalletSummaryDTO;
import com.homemakers.homemakers.dto.WithdrawalHistoryDTO;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final ProviderEarningRepository earningRepository;
    private final ProviderPayoutRepository payoutRepository;

    public WalletService(
            ProviderEarningRepository earningRepository,
            ProviderPayoutRepository payoutRepository
    ) {
        this.earningRepository = earningRepository;
        this.payoutRepository = payoutRepository;
    }

    // =========================
    // WALLET SUMMARY
    // =========================
    public WalletSummaryDTO getSummary(Provider provider) {

        WalletSummaryDTO dto = new WalletSummaryDTO();

        Double available = earningRepository.sumByProviderIdAndStatus(
                provider.getId(),
                EarningStatus.AVAILABLE
        );

        Double requested = earningRepository.sumByProviderIdAndStatus(
                provider.getId(),
                EarningStatus.REQUESTED
        );

        Double paid = earningRepository.sumByProviderIdAndStatus(
                provider.getId(),
                EarningStatus.PAID
        );
        dto.setAvailable(normalize(available));
        dto.setRequested(normalize(requested));
        dto.setPaid(normalize(paid));

        ProviderPayout lastPaid =
                payoutRepository.findTopByProviderAndStatusOrderByPaidAtDesc(
                        provider, PayoutStatus.PAID);

        dto.setAvailable(available);
        dto.setRequested(requested);
        dto.setPaid(paid);
        dto.setLastPayoutDate(
                lastPaid != null ? lastPaid.getPaidAt() : null
        );

        // =========================
        // WEEKLY WITHDRAWAL ELIGIBILITY
        // =========================
        LocalDateTime lastRequested = provider.getLastPayoutRequestedAt();

        if (lastRequested == null) {

            dto.setCanWithdraw(available > 0);
            dto.setNextEligibleWithdrawalDate(null);

        } else {

            LocalDateTime nextEligible =
                    lastRequested.plusDays(7);

            boolean canWithdraw =
                    LocalDateTime.now().isAfter(nextEligible)
                            && available > 0;

            dto.setCanWithdraw(canWithdraw);
            dto.setNextEligibleWithdrawalDate(nextEligible);
        }
        System.out.println("Logged in provider id = " + provider.getId());
        System.out.println("Provider ID = " + provider.getId());

        Double debugSum = earningRepository.sumByProviderIdAndStatus(
                provider.getId(),
                EarningStatus.AVAILABLE
        );

        System.out.println("DEBUG SUM = " + debugSum);
        return dto;
    }

    // =========================
    // WITHDRAWAL HISTORY
    // =========================
    public List<WithdrawalHistoryDTO> getWithdrawalHistory(Provider provider) {

        return payoutRepository
                .findByProviderOrderByCreatedAtDesc(provider)
                .stream()
                .map(p -> {
                    WithdrawalHistoryDTO dto = new WithdrawalHistoryDTO();

                    dto.setPayoutId(p.getId());
                    dto.setAmount(p.getAmount());
                    dto.setStatus(p.getStatus().name());
                    dto.setCreatedAt(p.getCreatedAt());
                    dto.setPaidAt(p.getPaidAt());

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // =========================
    // HELPER METHOD
    // =========================
    private double normalize(Double value) {
        return value != null ? value : 0.0;
    }
}