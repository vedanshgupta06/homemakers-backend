package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.PayoutStatus;
import com.homemakers.homemakers.model.ProviderPayout;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.homemakers.homemakers.dto.AdminPayoutDTO;
import java.util.List;
@Service
public class AdminPayoutService {

    private final ProviderPayoutRepository payoutRepository;

    public AdminPayoutService(ProviderPayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

    // =========================
    // ADMIN VIEW ALL PAYOUTS
    // =========================
    public List<AdminPayoutDTO> getAllPayoutsForAdmin() {
        return payoutRepository.findAll().stream().map(p -> {
            AdminPayoutDTO dto = new AdminPayoutDTO();

            dto.setId(p.getId());
            dto.setProviderId(p.getProvider().getId());
            dto.setPayoutMonth(p.getPayoutMonth());
            dto.setTotalEarnings(p.getTotalEarnings());
            dto.setTotalDeductions(p.getTotalDeductions());
            dto.setNetPayout(p.getNetPayout());
            dto.setStatus(p.getStatus().name());

            return dto;
        }).toList();
    }

    // =========================
    // MARK PAYOUT AS PAID
    // =========================
    public ProviderPayout markPayoutAsPaid(Long payoutId) {
        ProviderPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        if (payout.getStatus() != PayoutStatus.CALCULATED) {
            throw new RuntimeException("Only CALCULATED payouts can be marked PAID");
        }

        payout.markPaid();
        return payoutRepository.save(payout);
    }
}
