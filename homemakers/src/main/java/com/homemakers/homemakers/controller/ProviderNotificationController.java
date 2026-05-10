package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/provider/notifications")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderNotificationController {

    private final ProviderNotificationRepository notificationRepository;
    private final ProviderRepository providerRepository;

    public ProviderNotificationController(
            ProviderNotificationRepository notificationRepository,
            ProviderRepository providerRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public List<ProviderNotification> getNotifications(Authentication auth) {
        Provider provider = providerRepository
                .findByUserEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return notificationRepository.findByProviderOrderByCreatedAtDesc(provider);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(Authentication auth) {
        Provider provider = providerRepository
                .findByUserEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        long count = notificationRepository.countByProviderAndReadFalse(provider);
        return Map.of("count", count);
    }

    @PutMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @PutMapping("/read-all")
    public void markAllRead(Authentication auth) {
        Provider provider = providerRepository
                .findByUserEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        List<ProviderNotification> unread = notificationRepository
                .findByProviderOrderByCreatedAtDesc(provider)
                .stream()
                .filter(n -> !n.isRead())
                .toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}