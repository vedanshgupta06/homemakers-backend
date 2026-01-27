package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.AvailabilityRequest;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.ProviderAvailabilityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/provider/availability")
public class ProviderAvailabilityController {

    private final ProviderAvailabilityService availabilityService;
    private final ProviderRepository providerRepository;

    public ProviderAvailabilityController(
            ProviderAvailabilityService availabilityService,
            ProviderRepository providerRepository
    ) {
        this.availabilityService = availabilityService;
        this.providerRepository = providerRepository;
    }

    // ===============================
    // PROVIDER → add availability
    // ===============================
    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public String addAvailability(@RequestBody AvailabilityRequest request) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        availabilityService.addAvailability(provider, request);

        return "Availability added successfully";
    }

    // ===============================
    // PROVIDER → view own availability
    // ===============================
    @GetMapping("/my")
    @PreAuthorize("hasRole('PROVIDER')")
    public Object getMyAvailability() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return availabilityService.getAvailabilityForProvider(provider.getId());
    }

    // ===============================
    // USER → view provider availability
    // ===============================
    @GetMapping("/{providerId}")
    @PreAuthorize("hasRole('USER')")
    public Object getAvailability(@PathVariable Long providerId) {
        return availabilityService.getAvailabilityForProvider(providerId);
    }
}
