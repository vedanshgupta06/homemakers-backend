package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.ProviderPayoutTransaction;
import com.homemakers.homemakers.service.AdminPayoutTransactionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/admin/weekly-payouts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPayoutTransactionController {

    private final AdminPayoutTransactionService payoutService;

    public AdminPayoutTransactionController(
            AdminPayoutTransactionService payoutService
    ) {
        this.payoutService = payoutService;
    }

    // VIEW REQUESTED WEEKLY PAYOUTS
    @GetMapping("/requested")
    public List<ProviderPayoutTransaction> getRequestedPayouts() {
        return payoutService.getRequestedPayouts();
    }

    // MARK WEEKLY PAYOUT AS PAID
    @PutMapping("/{id}/pay")
    public ProviderPayoutTransaction markAsPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String referenceId
    ) {
        return payoutService.markAsPaid(id, referenceId);
    }
}
