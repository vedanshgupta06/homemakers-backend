package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.SettlementPreview;
import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.ProviderSettlementPreviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/provider/settlement")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderSettlementController {

    private final ProviderSettlementPreviewService previewService;
    private final ProviderRepository providerRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ProviderSettlementController(
            ProviderSettlementPreviewService previewService,
            ProviderRepository providerRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository
    ) {
        this.previewService = previewService;
        this.providerRepository = providerRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{bookingId}")
    public Map<String, Object> getPreview(
            @PathVariable Long bookingId,
            Principal principal
    ) {
        // 1️⃣ Logged-in user
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        // 2️⃣ Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 3️⃣ SECURITY CHECK (IMPORTANT)
        if (!booking.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("You are not allowed to view this booking");
        }

        // 4️⃣ Calculation
        SettlementPreview preview =
                previewService.calculatePreview(provider, booking);

        // 5️⃣ Return combined response (NO new DTO needed)
        return Map.of(
                "bookingId", booking.getId(),
                "bookingStatus", booking.getStatus(),
                "preview", preview
        );
    }
}
