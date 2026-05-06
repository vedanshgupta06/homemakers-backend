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

        double available = normalize(earningRepository.sumByProviderIdAndStatus(
                provider.getId(), EarningStatus.AVAILABLE));

        double penalty = normalize(earningRepository.sumByProviderIdAndStatus(
                provider.getId(), EarningStatus.PENALTY)); // already negative

        double requested = normalize(earningRepository.sumByProviderIdAndStatus(
                provider.getId(), EarningStatus.REQUESTED));

        double paid = normalize(earningRepository.sumByProviderIdAndStatus(
                provider.getId(), EarningStatus.PAID));

        double netAvailable = available + penalty; // penalty is negative so it subtracts

        dto.setAvailable(netAvailable);
        dto.setRequested(requested);
        dto.setPaid(paid);

        ProviderPayout lastPaid =
                payoutRepository.findTopByProviderAndStatusOrderByPaidAtDesc(
                        provider, PayoutStatus.PAID);

        dto.setLastPayoutDate(lastPaid != null ? lastPaid.getPaidAt() : null);

        // =========================
        // WEEKLY WITHDRAWAL ELIGIBILITY
        // =========================
        LocalDateTime lastRequested = provider.getLastPayoutRequestedAt();

        if (lastRequested == null) {
            dto.setCanWithdraw(netAvailable > 0);
            dto.setNextEligibleWithdrawalDate(null);
        } else {
            LocalDateTime nextEligible = lastRequested.plusDays(7);
            boolean canWithdraw = LocalDateTime.now().isAfter(nextEligible) && netAvailable > 0;
            dto.setCanWithdraw(canWithdraw);
            dto.setNextEligibleWithdrawalDate(nextEligible);
        }

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