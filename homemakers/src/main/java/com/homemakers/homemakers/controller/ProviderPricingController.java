package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.PricingRequest;
import com.homemakers.homemakers.model.ServicePricing;
import com.homemakers.homemakers.service.ProviderPricingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/provider/pricing")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderPricingController {

    private final ProviderPricingService pricingService;

    public ProviderPricingController(ProviderPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping
    public List<ServicePricing> getMyPricing() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return pricingService.getMyPricing(email);
    }


    @PostMapping
    public ServicePricing savePricing(
            @RequestBody PricingRequest request
    ) {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return pricingService.savePricing(
                email,
                request.getService(),
                request.getPricingType(),   // ✅ FIXED
                request.getPrice()          // ✅ FIXED
        );
    }
}
