package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.AvailabilityRequest;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderAvailability;
import com.homemakers.homemakers.repository.ProviderAvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProviderAvailabilityService {

    private final ProviderAvailabilityRepository availabilityRepo;

    public ProviderAvailabilityService(
            ProviderAvailabilityRepository availabilityRepo
    ) {
        this.availabilityRepo = availabilityRepo;
    }

    public void addAvailability(
            Provider provider,
            AvailabilityRequest request
    ) {

        if (request.getDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot add availability for past dates");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        boolean overlap =
                availabilityRepo
                        .existsByProviderAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                                provider,
                                request.getDate(),
                                request.getEndTime(),
                                request.getStartTime()
                        );

        if (overlap) {
            throw new RuntimeException("Availability overlaps with existing slot");
        }

        ProviderAvailability availability = new ProviderAvailability();
        availability.setProvider(provider);
        availability.setDate(request.getDate());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());

        availabilityRepo.save(availability);
    }
    public List<ProviderAvailability> getAvailabilityForProvider(Long providerId) {
        return availabilityRepo.findByProvider_Id(providerId);
    }
}
