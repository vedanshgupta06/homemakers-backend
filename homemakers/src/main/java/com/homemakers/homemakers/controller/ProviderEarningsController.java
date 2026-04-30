package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

        double available = earningRepository
                .findByProviderAndStatus(provider, EarningStatus.AVAILABLE)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();


        double paid = earningRepository
                .findByProviderAndStatus(provider, EarningStatus.PAID)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        Map<String, Double> res = new HashMap<>();
        res.put("pending", available);
        res.put("paid", paid);
        res.put("total", available + paid);

        return res;
    }
    @GetMapping("/stats")
    public Map<String, Double> stats() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        List<ProviderEarning> earnings = earningRepository.findByProvider(provider);

        double today = 0;
        double week = 0;
        double month = 0;

        java.time.LocalDate todayDate = java.time.LocalDate.now();

        for (ProviderEarning e : earnings) {

            java.time.LocalDate d = e.getWorkDate();

            if (d.equals(todayDate)) {
                today += e.getAmount();
            }

            if (!d.isBefore(todayDate.minusDays(7))) {
                week += e.getAmount();
            }

            if (d.getMonth() == todayDate.getMonth()) {
                month += e.getAmount();
            }
        }

        Map<String, Double> res = new HashMap<>();
        res.put("today", today);
        res.put("week", week);
        res.put("month", month);

        return res;
    }
    // In your ProviderController or EarningsController
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyEarnings() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Provider provider = providerRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        List<ProviderEarning> earnings =
                earningRepository.findByProvider_User_EmailOrderByWorkDateDesc(email);

        // ✅ Map to simple objects — avoids lazy-load serialization error
        List<Map<String, Object>> result = earnings.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", e.getId());
            map.put("amount", e.getAmount());
            map.put("status", e.getStatus());
            map.put("workDate", e.getWorkDate());
            map.put("reason", e.getReason());
            map.put("bookingId", e.getBooking() != null ? e.getBooking().getId() : null);
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }
}
