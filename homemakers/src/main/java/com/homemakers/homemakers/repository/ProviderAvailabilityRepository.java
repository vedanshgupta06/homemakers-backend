package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ProviderAvailabilityRepository
        extends JpaRepository<ProviderAvailability, Long> {

    List<ProviderAvailability> findByProviderAndDateAndActiveTrue(
            Provider provider,
            LocalDate date
    );

    boolean existsByProviderAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Provider provider,
            LocalDate date,
            LocalTime endTime,
            LocalTime startTime
    );

    List<ProviderAvailability> findByProvider_Id(Long providerId);

    boolean existsByProviderAndActiveTrue(Provider provider);

}
