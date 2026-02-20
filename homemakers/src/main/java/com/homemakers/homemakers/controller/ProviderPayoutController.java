package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderPayout;
import com.homemakers.homemakers.repository.ProviderPayoutRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;


import java.util.List;

@RestController
@RequestMapping("/api/provider/payouts")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderPayoutController {

    private final ProviderRepository providerRepository;
    private final ProviderPayoutRepository payoutRepository;

    public ProviderPayoutController(
            ProviderRepository providerRepository,
            ProviderPayoutRepository payoutRepository
    ) {
        this.providerRepository = providerRepository;
        this.payoutRepository = payoutRepository;
    }

    @GetMapping
    public List<ProviderPayout> getMyPayouts(Authentication authentication) {

        String email = authentication.getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return payoutRepository
                .findByProviderOrderByCreatedAtDesc(provider);
    }
}
