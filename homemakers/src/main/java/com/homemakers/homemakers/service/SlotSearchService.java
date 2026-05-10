package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.AvailableSlotResponse;
import com.homemakers.homemakers.model.ProviderAvailability;
import com.homemakers.homemakers.repository.ProviderAvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotSearchService {

    private final ProviderAvailabilityRepository availabilityRepository;

    public SlotSearchService(ProviderAvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    public List<AvailableSlotResponse> findAvailableSlots(
            LocalDate date,
            int requiredMinutes
    ) {

        List<ProviderAvailability> slots =
                availabilityRepository.findByDateAndActiveTrue(date);

        List<AvailableSlotResponse> result = new ArrayList<>();

        for (ProviderAvailability slot : slots) {

            long slotMinutes =
                    Duration.between(
                            slot.getStartTime(),
                            slot.getEndTime()
                    ).toMinutes();

            if (slotMinutes >= requiredMinutes) {

                result.add(
                        new AvailableSlotResponse(
                                slot.getId(),
                                slot.getProvider().getId(),
                                slot.getProvider().getUser().getName(),
                                slot.getDate(),
                                slot.getStartTime(),
                                slot.getEndTime()
                        )
                );
            }
        }

        return result;
    }
}