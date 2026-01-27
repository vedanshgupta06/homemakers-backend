package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.ServicePricing;
import com.homemakers.homemakers.repository.ServicePricingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/providers")
public class UserProviderPricingController {

    private final ServicePricingRepository pricingRepository;

    public UserProviderPricingController(ServicePricingRepository pricingRepository) {
        this.pricingRepository = pricingRepository;
    }

    // ✅ USER → VIEW PROVIDER PRICING
    @GetMapping("/{providerId}/pricing")
    public List<ServicePricing> getProviderPricing(
            @PathVariable Long providerId
    ) {
        System.out.println("🔥 USER PRICING API HIT for providerId = " + providerId);
        return pricingRepository.findByProvider_Id(providerId);
    }

}
