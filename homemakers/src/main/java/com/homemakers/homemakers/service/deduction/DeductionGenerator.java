package com.homemakers.homemakers.service.deduction;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderDeductionRepository;
import com.homemakers.homemakers.repository.ProviderPenaltyLedgerRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.service.ProviderEarningService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeductionGenerator {

    private final ProviderRepository providerRepository;
    private final ProviderDeductionRepository deductionRepository;
    private final ProviderPenaltyLedgerRepository penaltyLedgerRepository;
    private final ProviderEarningService earningService;
    private final BookingRepository bookingRepository;

    public DeductionGenerator(
            ProviderRepository providerRepository,
            ProviderDeductionRepository deductionRepository,
            ProviderPenaltyLedgerRepository penaltyLedgerRepository,
            ProviderEarningService earningService,
            BookingRepository bookingRepository
    ) {
        this.providerRepository = providerRepository;
        this.deductionRepository = deductionRepository;
        this.penaltyLedgerRepository = penaltyLedgerRepository;
        this.earningService = earningService;
        this.bookingRepository = bookingRepository;
    }

    public ProviderDeduction generate(
            Long providerId,
            DeductionSourceType sourceType,
            Long sourceId,
            Long bookingId,
            DeductionResult result
    ) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Build descriptive reason with customer info
        String reason = "Complaint by " + booking.getUser().getName() +
                " for Booking #" + booking.getId() +
                " on " + booking.getAvailability().getDate() +
                " — " + result.getReason();

        // ── Step 1: Save deduction record ──
        ProviderDeduction deduction = new ProviderDeduction();
        deduction.setProvider(provider);
        deduction.setBooking(booking);
        deduction.setSourceType(sourceType);
        deduction.setSourceId(sourceId);
        deduction.setType(result.getDeductionType());
        deduction.setAmount(result.getAmount());
        deduction.setReason(reason);
        deduction.setState(DeductionState.PROPOSED);
        deduction.setSystemGenerated(true);
        deductionRepository.save(deduction);

        // ── Step 2: Save penalty ledger (same as no-show flow) ──
        boolean alreadyPenalised = penaltyLedgerRepository.existsByBooking(booking);
        if (!alreadyPenalised) {

            ProviderPenaltyLedger penalty = new ProviderPenaltyLedger();
            penalty.setProvider(provider);
            penalty.setBooking(booking);
            penalty.setPenaltyAmount(result.getAmount());
            penalty.setReason(reason);
            penalty.setCreatedAt(LocalDateTime.now());
            penalty.setDeducted(true);
            penaltyLedgerRepository.save(penalty);

            // ── Step 3: Apply to wallet via earning service ──
            earningService.addPenaltyEarning(
                    provider,
                    booking,
                    result.getAmount(),
                    reason
            );
        }

        return deduction;
    }
}