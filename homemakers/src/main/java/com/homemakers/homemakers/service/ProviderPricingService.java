package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.PricingType;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ServicePricing;
import com.homemakers.homemakers.model.ServiceType;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.ServicePricingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ProviderPricingService {

    private final ServicePricingRepository pricingRepository;
    private final ProviderRepository providerRepository;

    public ProviderPricingService(
            ServicePricingRepository pricingRepository,
            ProviderRepository providerRepository
    ) {
        this.pricingRepository = pricingRepository;
        this.providerRepository = providerRepository;
    }
    @Transactional
    public ServicePricing savePricing(
            String providerEmail,
            ServiceType service,
            PricingType pricingType,
            double price
    ) {
        Provider provider = providerRepository
                .findByUser_Email(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        String city = provider.getCity();
        if (city == null || city.isBlank()) {
            throw new RuntimeException("Provider city is missing");
        }

        ServicePricing pricing = pricingRepository
                .findByProviderAndServiceAndCity(provider, service, city)
                .orElseGet(() -> {
                    ServicePricing sp = new ServicePricing();
                    sp.setProvider(provider);
                    sp.setService(service);
                    sp.setCity(city); // ✅ SET IMMEDIATELY
                    return sp;
                });

        pricing.setPricingType(pricingType);

        if (pricingType == PricingType.HOURLY_MONTHLY) {
            pricing.setPricePerHour(price);
        } else {
            pricing.setMonthlyRate(price);
        }

        return pricingRepository.save(pricing);
    }
    public List<ServicePricing> getMyPricing(String providerEmail) {
        Provider provider = providerRepository
                .findByUser_Email(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return pricingRepository.findByProvider(provider);
    }

}
