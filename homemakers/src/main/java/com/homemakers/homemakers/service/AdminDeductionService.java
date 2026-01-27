package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.DeductionState;
import com.homemakers.homemakers.model.ProviderDeduction;
import com.homemakers.homemakers.repository.ProviderDeductionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminDeductionService {

    private final ProviderDeductionRepository deductionRepository;

    public AdminDeductionService(ProviderDeductionRepository deductionRepository) {
        this.deductionRepository = deductionRepository;
    }

    @Transactional
    public ProviderDeduction approveDeduction(Long deductionId, String note) {

        ProviderDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new RuntimeException("Deduction not found"));

        if (deduction.getState() != DeductionState.PROPOSED) {
            throw new IllegalStateException("Only PROPOSED deductions can be approved");
        }

        deduction.setState(DeductionState.APPROVED);
        deduction.setReason(note);
        deduction.setResolvedAt(LocalDateTime.now());

        return deduction;
    }

    @Transactional
    public ProviderDeduction waiveDeduction(Long deductionId, String note) {

        ProviderDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new RuntimeException("Deduction not found"));

        if (deduction.getState() != DeductionState.PROPOSED) {
            throw new IllegalStateException("Only PROPOSED deductions can be waived");
        }

        deduction.setState(DeductionState.WAIVED);
        deduction.setReason(note);
        deduction.setResolvedAt(LocalDateTime.now());

        return deduction;
    }
}
