package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import com.homemakers.homemakers.service.ProviderWorkLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/provider/attendance")
@CrossOrigin
public class ProviderAttendanceController {

    private final ProviderWorkLogService workLogService;
    private final ProviderRepository providerRepository;
    private final ProviderWorkLogRepository workLogRepository;

    public ProviderAttendanceController(
            ProviderWorkLogService workLogService,
            ProviderRepository providerRepository,
            ProviderWorkLogRepository workLogRepository
    ) {
        this.workLogService = workLogService;
        this.providerRepository = providerRepository;
        this.workLogRepository = workLogRepository;
    }

    @PutMapping("/{id}/mark-present")
    public ResponseEntity<?> markPresent(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String email = authentication.getName();

        Provider provider = providerRepository
                .findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        workLogService.markPresent(id, provider);

        return ResponseEntity.ok("Marked as present");
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayAttendance(Authentication authentication) {

        String email = authentication.getName();

        Provider provider = providerRepository
                .findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return ResponseEntity.ok(
                workLogRepository.findByProviderAndWorkDate(provider, LocalDate.now())
        );
    }
}