package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.AdminPayoutDTO;
import com.homemakers.homemakers.model.ProviderPayout;
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
    // GET ALL PAYOUTS (ADMIN)
    // =========================
    @GetMapping
    public List<AdminPayoutDTO> getAllPayouts() {
        return payoutService.getAllPayoutsForAdmin();
    }

    // =========================
    // MARK PAYOUT AS PAID
    // =========================
    @PutMapping("/{payoutId}/mark-paid")
    public void markPayoutAsPaid(@PathVariable Long payoutId) {
        payoutService.markPayoutAsPaid(payoutId);
    }
}
