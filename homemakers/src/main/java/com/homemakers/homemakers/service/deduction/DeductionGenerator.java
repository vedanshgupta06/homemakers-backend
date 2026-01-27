package com.homemakers.homemakers.service.deduction;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderDeductionRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import org.springframework.stereotype.Service;

@Service
public class DeductionGenerator {

    private final ProviderRepository providerRepository;
    private final ProviderDeductionRepository deductionRepository;

    public DeductionGenerator(
            ProviderRepository providerRepository,
            ProviderDeductionRepository deductionRepository
    ) {
        this.providerRepository = providerRepository;
        this.deductionRepository = deductionRepository;
    }

    public ProviderDeduction generate(
            Long providerId,
            DeductionSourceType sourceType,
            Long sourceId,
            DeductionResult result
    ) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        ProviderDeduction deduction = new ProviderDeduction();
        deduction.setProvider(provider);
        deduction.setSourceType(sourceType);
        deduction.setSourceId(sourceId);
        deduction.setType(result.getDeductionType());
        deduction.setAmount(result.getAmount());
        deduction.setReason(result.getReason());
        deduction.setState(DeductionState.PROPOSED);
        deduction.setSystemGenerated(true);

        return deductionRepository.save(deduction);
    }
}
