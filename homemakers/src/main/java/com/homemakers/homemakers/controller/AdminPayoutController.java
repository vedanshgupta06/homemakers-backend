package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.AdminPayoutDTO;
import com.homemakers.homemakers.service.AdminPayoutService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payouts")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPayoutController {

    private final AdminPayoutService payoutService;

    public AdminPayoutController(AdminPayoutService payoutService) {
        this.payoutService = payoutService;
    }

    // =========================
    // PENDING PAYOUT REQUESTS
    // =========================
    @GetMapping("/requests")
    public List<AdminPayoutDTO> getRequestedPayouts() {
        return payoutService.getRequestedPayouts();
    }

    // =========================
    // PAYOUT HISTORY
    // =========================
    @GetMapping("/history")
    public List<AdminPayoutDTO> getPayoutHistory() {
        return payoutService.getPayoutHistory();
    }

    // =========================
    // MARK PAYOUT AS PAID
    // =========================
    @PutMapping("/{payoutId}/mark-paid")
    public void markPayoutAsPaid(@PathVariable Long payoutId) {
        payoutService.markPayoutAsPaid(payoutId);
    }
}