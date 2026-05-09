package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.AvailabilityRequest;
import com.homemakers.homemakers.dto.AvailabilityResponse;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderAvailability;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderAvailabilityRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.service.ProviderAvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider/availability")
public class ProviderAvailabilityController {

    private final ProviderAvailabilityService availabilityService;
    private final ProviderRepository providerRepository;
    private final ProviderAvailabilityRepository availabilityRepository ;
    private final BookingRepository bookingRepository;
    public ProviderAvailabilityController(ProviderAvailabilityService availabilityService,
                                          ProviderRepository providerRepository,
                                          ProviderAvailabilityRepository availabilityRepository,
                                          BookingRepository bookingRepository) {
        this.availabilityService = availabilityService;
        this.providerRepository = providerRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
    }

    // ===============================
    // PROVIDER → add availability
    // ===============================
    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public String addAvailability(@RequestBody AvailabilityRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Provider provider = providerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        availabilityService.addAvailability(provider, request);
        return "Availability added successfully";
    }

    // ===============================
    // PROVIDER → view own availability
    // ===============================
    @GetMapping("/my")
    @PreAuthorize("hasRole('PROVIDER')")
    public List<AvailabilityResponse> getMyAvailability() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Provider provider = providerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return availabilityService.getAvailabilityForProvider(provider.getId());
    }

    // ===============================
    // USER → view provider availability
    // ===============================
    @GetMapping("/{providerId}")
    @PreAuthorize("hasRole('USER')")
    public List<AvailabilityResponse> getAvailability(@PathVariable Long providerId) {
        return availabilityService.getAvailabilityForProvider(providerId);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Long id,
            Authentication authentication) {

        ProviderAvailability slot = availabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        // Only allow the owning provider to delete
        if (!slot.getProvider().getUser().getEmail().equals(authentication.getName())) {
            throw new RuntimeException("Not authorized");
        }

        // Don't delete booked slots
        if (!slot.isActive()) {
            throw new RuntimeException("Cannot delete a booked slot");
        }

        // ✅ Null out any booking references before deleting
        // (cancelled bookings still hold a FK reference)
        bookingRepository.nullifyAvailability(id);

        availabilityRepository.delete(slot);
        return ResponseEntity.noContent().build();
    }
}