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

                    // Provider info
                    dto.setProviderId(p.getProvider().getId());
                    dto.setProviderName(
                            p.getProvider().getUser().getName()
                    );
                    dto.setProviderEmail(
                            p.getProvider().getUser().getEmail()
                    );

                    dto.setAmount(p.getAmount());
                    dto.setStatus(p.getStatus().name());

                    dto.setCreatedAt(p.getCreatedAt());
                    dto.setPaidAt(p.getPaidAt());

                    // Fetch earnings linked to this payout
                    List<ProviderEarning> earnings =
                            earningRepository.findByPayout(p);

                    if (!earnings.isEmpty()) {

                        ProviderEarning e = earnings.get(0);

                        dto.setWeekNo(e.getWeekNo());

                        if (e.getBooking() != null) {

                            dto.setBookingId(
                                    e.getBooking().getId()
                            );

                            // service name
                            if (e.getBooking().getServices() != null && !e.getBooking().getServices().isEmpty()) {

                                ServiceType service =
                                        e.getBooking().getServices().iterator().next();

                                dto.setServiceName(service.name());
                            }

                        }
                    }

                    return dto;

                })
                .toList();
    }

    // =========================
    // MARK PAYOUT AS PAID
    // =========================
    @Transactional
    public ProviderPayout markPayoutAsPaid(Long payoutId) {

        ProviderPayout payout = payoutRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        if (payout.getStatus() != PayoutStatus.INITIATED) {
            throw new RuntimeException("Only INITIATED payouts can be marked PAID");
        }

        List<ProviderEarning> earnings =
                earningRepository.findByPayoutForUpdate(payout);

        if (earnings.isEmpty()) {
            throw new RuntimeException("No earnings linked to this payout");
        }

        for (ProviderEarning earning : earnings) {

            if (earning.getStatus() != EarningStatus.REQUESTED) {
                throw new RuntimeException(
                        "Invalid earning state. Expected REQUESTED but found: "
                                + earning.getStatus()
                );
            }

            earning.setStatus(EarningStatus.PAID);
        }

        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(LocalDateTime.now());

        return payout;
    }

    // =========================
    // REJECT PAYOUT
    // =========================
    @Transactional
    public ProviderPayout rejectPayout(Long payoutId, String reason) {

        ProviderPayout payout = payoutRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        if (payout.getStatus() != PayoutStatus.INITIATED) {
            throw new RuntimeException("Only INITIATED payouts can be rejected");
        }

        List<ProviderEarning> earnings =
                earningRepository.findByPayoutForUpdate(payout);

        for (ProviderEarning earning : earnings) {

            if (earning.getStatus() != EarningStatus.REQUESTED) {
                throw new RuntimeException("Invalid earning state during rejection");
            }

            // Return earnings to wallet
            earning.setStatus(EarningStatus.AVAILABLE);
            earning.setPayout(null);
        }

        payout.setStatus(PayoutStatus.REJECTED);
        payout.setPaidAt(null);

        return payout;
    }
}