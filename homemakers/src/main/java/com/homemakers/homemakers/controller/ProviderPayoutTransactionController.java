package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.RequestPayoutRequest;
import com.homemakers.homemakers.dto.WeeklySummaryResponse;
import com.homemakers.homemakers.model.ProviderPayoutTransaction;
import com.homemakers.homemakers.service.ProviderPayoutTransactionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/provider/payouts")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderPayoutTransactionController {

    private final ProviderPayoutTransactionService payoutService;

    public ProviderPayoutTransactionController(
            ProviderPayoutTransactionService payoutService
    ) {
        this.payoutService = payoutService;
    }

    // ===============================
    // REQUEST WEEKLY PAYOUT
    // ===============================
    @PostMapping("/request")
    public ProviderPayoutTransaction requestPayout(
            @RequestBody RequestPayoutRequest request
    ) {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return payoutService.requestPayout(
                email,
                request.getBookingId(),
                request.getWeekNo(),
                request.getAmount()   // ✅ THIS WAS MISSING
        );
    }

    // ===============================
    // SERVICE-WISE WEEKLY SUMMARY
    // ===============================
    @GetMapping("/weekly-summary")
    public List<WeeklySummaryResponse> getWeeklySummary() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return payoutService.getWeeklySummary(email);
    }
}
