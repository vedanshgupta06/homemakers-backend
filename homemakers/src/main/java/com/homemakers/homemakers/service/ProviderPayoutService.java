package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderPayout;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProviderPayoutService {

    private final ProviderPayoutRepository payoutRepository;

    public ProviderPayoutService(ProviderPayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

    public List<ProviderPayout> getMyPayouts(Provider provider) {
        return payoutRepository.findByProviderOrderByPayoutMonthDesc(provider);
    }

}