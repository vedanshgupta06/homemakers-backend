package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.CustomerAttendanceDTO;
import com.homemakers.homemakers.model.ProviderWorkLog;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.ProviderWorkLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customer/attendance")
@PreAuthorize("hasRole('USER')")
public class CustomerAttendanceController {

    private final ProviderWorkLogService workLogService;
    private final UserRepository userRepository;
    private final ProviderWorkLogRepository workLogRepository;

    public CustomerAttendanceController(
            ProviderWorkLogService workLogService,
            UserRepository userRepository,
            ProviderWorkLogRepository workLogRepository
    ) {
        this.workLogService = workLogService;
        this.userRepository = userRepository;
        this.workLogRepository = workLogRepository;
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayAttendance(Authentication authentication) {

        String email = authentication.getName();

        User customer = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var logs = workLogRepository
                .findByBookingUserAndWorkDate(customer, LocalDate.now())
                .stream()
                .map(log -> new CustomerAttendanceDTO(
                        log.getId(),
                        log.getBooking().getId(),
                        log.getProvider().getUser().getName(),
                        log.getWorkDate(),
                        log.getStatus().name()
                ))
                .toList();

        return ResponseEntity.ok(logs);
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