package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.WalletSummaryDTO;
import com.homemakers.homemakers.dto.WithdrawalHistoryDTO;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.service.WalletService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/provider/wallet")
@PreAuthorize("hasRole('PROVIDER')")
public class WalletController {

    private final WalletService walletService;
    private final ProviderRepository providerRepository;

    public WalletController(
            WalletService walletService,
            ProviderRepository providerRepository
    ) {
        this.walletService = walletService;
        this.providerRepository = providerRepository;
    }

    @GetMapping("/summary")
    public WalletSummaryDTO getSummary(Authentication authentication) {

        String email = authentication.getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return walletService.getSummary(provider);
    }
    @GetMapping("/withdrawals")
    public List<WithdrawalHistoryDTO> getWithdrawals(Authentication authentication) {

        String email = authentication.getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return walletService.getWithdrawalHistory(provider);
    }

}
