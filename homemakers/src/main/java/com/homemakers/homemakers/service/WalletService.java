package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.WalletSummaryDTO;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import org.springframework.stereotype.Service;
import com.homemakers.homemakers.dto.WithdrawalHistoryDTO;

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

    public WalletSummaryDTO getSummary(Provider provider) {

        WalletSummaryDTO dto = new WalletSummaryDTO();

        Double available = earningRepository.sumByProviderAndStatus(
                provider, EarningStatus.AVAILABLE);

        Double requested = earningRepository.sumByProviderAndStatus(
                provider, EarningStatus.REQUESTED);

        Double paid = earningRepository.sumByProviderAndStatus(
                provider, EarningStatus.PAID);

        ProviderPayout lastPaid =
                payoutRepository.findTopByProviderAndStatusOrderByPaidAtDesc(
                        provider, PayoutStatus.PAID);

        dto.setAvailable(available != null ? available : 0);
        dto.setRequested(requested != null ? requested : 0);
        dto.setPaid(paid != null ? paid : 0);
        dto.setLastPayoutDate(
                lastPaid != null ? lastPaid.getPaidAt() : null
        );

        return dto;
    }


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

}
