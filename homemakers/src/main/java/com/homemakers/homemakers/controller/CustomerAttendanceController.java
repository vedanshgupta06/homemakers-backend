package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.ProviderWorkLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/customer/attendance")
@PreAuthorize("hasRole('USER')")
public class CustomerAttendanceController {

    private final ProviderWorkLogService workLogService;
    private final UserRepository userRepository;

    public CustomerAttendanceController(
            ProviderWorkLogService workLogService,
            UserRepository userRepository
    ) {
        this.workLogService = workLogService;
        this.userRepository = userRepository;
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmAttendance(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User customer = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        workLogService.confirmAttendance(id, customer);

        return ResponseEntity.ok("Attendance confirmed");
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectAttendance(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User customer = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        workLogService.rejectAttendance(id, customer);

        return ResponseEntity.ok("Attendance rejected");
    }
}