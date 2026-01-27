package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/provider/earnings")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderEarningsController {

    private final ProviderRepository providerRepository;
    private final ProviderEarningRepository earningRepository;

    public ProviderEarningsController(
            ProviderRepository providerRepository,
            ProviderEarningRepository earningRepository
    ) {
        this.providerRepository = providerRepository;
        this.earningRepository = earningRepository;
    }

    // ===============================
    // LIST ALL EARNINGS
    // ===============================
    @GetMapping("/list")
    public List<ProviderEarning> list() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return earningRepository.findByProvider(provider);
    }

    // ===============================
    // SUMMARY (Total / Pending / Paid)
    // ===============================
    @GetMapping("/summary")
    public Map<String, Double> summary() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        double pending = earningRepository
                .findByProviderAndStatus(provider, EarningStatus.PENDING)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        double paid = earningRepository
                .findByProviderAndStatus(provider, EarningStatus.PAID)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        Map<String, Double> res = new HashMap<>();
        res.put("pending", pending);
        res.put("paid", paid);
        res.put("total", pending + paid);

        return res;
    }
}
