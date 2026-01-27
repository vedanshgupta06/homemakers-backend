package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ServicePricing;
import com.homemakers.homemakers.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicePricingRepository
        extends JpaRepository<ServicePricing, Long> {

    Optional<ServicePricing> findByProviderAndServiceAndCity(
            Provider provider,
            ServiceType service,
            String city
    );

    Optional<ServicePricing> findByProviderAndService(
            Provider provider,
            ServiceType service
    );


    List<ServicePricing> findByProvider(Provider provider);
    List<ServicePricing> findByProvider_Id(Long providerId);

}
