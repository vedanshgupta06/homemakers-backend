package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.AdminPayoutDTO;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderEarningRepository;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminPayoutService {

    private final ProviderPayoutRepository payoutRepository;
    private final ProviderEarningRepository earningRepository;

    public AdminPayoutService(
            ProviderPayoutRepository payoutRepository,
            ProviderEarningRepository earningRepository
    ) {
        this.payoutRepository = payoutRepository;
        this.earningRepository = earningRepository;
    }

    // =========================
    // ADMIN VIEW ALL PAYOUTS
    // =========================
    public List<AdminPayoutDTO> getAllPayoutsForAdmin() {

        return payoutRepository.findAll()
                .stream()
                .map(p -> {
                    AdminPayoutDTO dto = new AdminPayoutDTO();

                    dto.setId(p.getId());
                    dto.setProviderId(p.getProvider().getId());
                    dto.setAmount(p.getAmount());
                    dto.setStatus(p.getStatus().name());
                    dto.setCreatedAt(p.getCreatedAt());
                    dto.setPaidAt(p.getPaidAt());

                    return dto;
                })
                .toList();
    }


    // =========================
    // MARK PAYOUT AS PAID
    // =========================
    @Transactional
    public ProviderPayout markPayoutAsPaid(Long payoutId) {

        ProviderPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        if (payout.getStatus() != PayoutStatus.INITIATED) {
            throw new RuntimeException("Only INITIATED payouts can be marked PAID");
        }

        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(LocalDateTime.now());

        // Move linked earnings → PAID
        List<ProviderEarning> earnings =
                earningRepository.findByPayout(payout);

        for (ProviderEarning earning : earnings) {
            earning.setStatus(EarningStatus.PAID);
        }

        return payout;
    }
}
