package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderPayout;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.service.ProviderPayoutService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/provider/payouts")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderPayoutController {

    private final ProviderRepository providerRepository;
    private final ProviderPayoutService payoutService;

    public ProviderPayoutController(
            ProviderRepository providerRepository,
            ProviderPayoutService payoutService
    ) {
        this.providerRepository = providerRepository;
        this.payoutService = payoutService;
    }

    @GetMapping
    public List<ProviderPayout> getMyPayouts(Authentication authentication) {

        Provider provider = getProviderFromAuth(authentication);

        return payoutService.getPayoutHistory(provider);
    }

    @PostMapping("/request")
    public ProviderPayout requestPayout(Authentication authentication) {

        Provider provider = getProviderFromAuth(authentication);

        return payoutService.requestPayout(provider);
    }

    private Provider getProviderFromAuth(Authentication authentication) {
        String email = authentication.getName();

        return providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }
}