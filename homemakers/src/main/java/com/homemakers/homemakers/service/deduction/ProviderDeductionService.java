package com.homemakers.homemakers.service.deduction;



import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderDeduction;
import com.homemakers.homemakers.repository.ProviderDeductionRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderDeductionService {

    private final ProviderRepository providerRepository;
    private final ProviderDeductionRepository deductionRepository;

    public ProviderDeductionService(
            ProviderRepository providerRepository,
            ProviderDeductionRepository deductionRepository
    ) {
        this.providerRepository = providerRepository;
        this.deductionRepository = deductionRepository;
    }

    public List<ProviderDeduction> getDeductionsForProvider(String email) {

        Provider provider = providerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return deductionRepository.findByProviderOrderByCreatedAtDesc(provider);
    }
}

