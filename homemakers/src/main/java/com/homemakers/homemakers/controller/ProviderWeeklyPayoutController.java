package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.ProviderPayoutTransaction;
import com.homemakers.homemakers.service.ProviderPayoutHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider/weekly-payouts")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderWeeklyPayoutController {

    private final ProviderPayoutHistoryService service;

    public ProviderWeeklyPayoutController(
            ProviderPayoutHistoryService service
    ) {
        this.service = service;
    }

    @GetMapping("/paid")
    public List<ProviderPayoutTransaction> getPaidWeeklyPayouts() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return service.getPaidWeeklyPayouts(email);
    }

}
