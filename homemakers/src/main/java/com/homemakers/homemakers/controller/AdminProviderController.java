package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.service.ProviderNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProviderController {

    private final ProviderRepository providerRepo;
    private final BookingRepository bookingRepository;
    private final ProviderEarningRepository earningRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final ProviderNotificationService notificationService; // ✅ use service not repo

    public AdminProviderController(
            ProviderRepository providerRepo,
            BookingRepository bookingRepository,
            ProviderEarningRepository earningRepository,
            ProviderAvailabilityRepository availabilityRepository,
            ProviderNotificationService notificationService // ✅
    ) {
        this.providerRepo = providerRepo;
        this.bookingRepository = bookingRepository;
        this.earningRepository = earningRepository;
        this.availabilityRepository = availabilityRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Provider> getAllProviders() {
        return providerRepo.findAll();
    }

    @GetMapping("/pending")
    public List<Provider> getPendingProviders() {
        return providerRepo.findByVerifiedFalse();
    }

    @GetMapping("/{id}/bookings")
    public List<Booking> getProviderBookings(@PathVariable Long id) {
        Provider provider = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return bookingRepository.findByProvider(provider);
    }

    @GetMapping("/{id}/earnings")
    public List<ProviderEarning> getProviderEarnings(@PathVariable Long id) {
        Provider provider = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return earningRepository.findByProvider(provider);
    }

    @GetMapping("/{id}/profile")
    public Provider getProviderProfile(@PathVariable Long id) {
        return providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    @GetMapping("/{id}/slots")
    public List<ProviderAvailability> getProviderSlots(@PathVariable Long id) {
        Provider provider = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return availabilityRepository.findByProvider(provider);
    }

    @PutMapping("/{id}/verify")
    public Provider verifyProvider(@PathVariable Long id) {
        Provider provider = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        provider.setVerified(true);
        providerRepo.save(provider);

        // ✅ Use notification service
        notificationService.send(
                provider,
                NotificationType.VERIFIED,
                "Account Verified ✅",
                "Congratulations! Your account has been verified by the admin. You can now receive bookings from customers."
        );

        return provider;
    }

    @PutMapping("/{id}/reject")
    public Provider rejectProvider(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        Provider provider = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        provider.setVerified(false);
        providerRepo.save(provider);

        String reason = body.getOrDefault("reason", "No reason provided.");

        // ✅ Use notification service
        notificationService.send(
                provider,
                NotificationType.REJECTED,
                "Verification Rejected ❌",
                "Your account verification was rejected by the admin. Reason: " + reason
        );

        return provider;
    }
}