package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderPayoutTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class AdminPayoutTransactionService {

    private final ProviderPayoutTransactionRepository transactionRepository;

    public AdminPayoutTransactionService(
            ProviderPayoutTransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }
    public List<ProviderPayoutTransaction> getRequestedPayouts() {
        return transactionRepository.findByStatus(WeeklyPayoutStatus.REQUESTED);
    }
    @Transactional
    public ProviderPayoutTransaction markAsPaid(
            Long payoutTransactionId,
            String referenceId
    ) {
        ProviderPayoutTransaction txn =
                transactionRepository.findById(payoutTransactionId)
                        .orElseThrow(() ->
                                new RuntimeException("Payout transaction not found"));

        if (txn.getStatus() != WeeklyPayoutStatus.REQUESTED) {
            throw new RuntimeException("Only PENDING payouts can be paid");
        }

        txn.setStatus(WeeklyPayoutStatus.PAID);
        txn.setPaidAt(LocalDateTime.now());
        txn.setReferenceId(referenceId);

        return transactionRepository.save(txn);
    }

}
