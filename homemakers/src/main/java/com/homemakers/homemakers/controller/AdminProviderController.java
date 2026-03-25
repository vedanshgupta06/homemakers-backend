package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.repository.ProviderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProviderController {

    private final ProviderRepository providerRepo;

    public AdminProviderController(ProviderRepository providerRepo) {
        this.providerRepo = providerRepo;
    }
    @GetMapping
    public List<Provider> getAllProviders() {
        return providerRepo.findAll();
    }
    @GetMapping("/pending")
    public List<Provider> getPendingProviders() {
        return providerRepo.findByVerifiedFalse();
    }

    @PutMapping("/{id}/verify")
    public Provider verifyProvider(@PathVariable Long id) {

        Provider provider = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        provider.setVerified(true);

        return providerRepo.save(provider);
    }
}
