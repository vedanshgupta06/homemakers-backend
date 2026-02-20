package com.homemakers.homemakers.controller;


import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final BookingRepository bookingRepository;
    private final ProviderRepository providerRepository;
    private final ProviderEarningRepository earningRepository;

    public AdminAnalyticsController(
            BookingRepository bookingRepository,
            ProviderRepository providerRepository,
            ProviderEarningRepository earningRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.providerRepository = providerRepository;
        this.earningRepository = earningRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        double totalRevenue = earningRepository
                .findByStatus(EarningStatus.PAID)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        long activeBookings = bookingRepository
                .findByStatus(BookingStatus.CONFIRMED)
                .size();

        double pendingPayout = earningRepository
                .findByStatus(EarningStatus.AVAILABLE)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        long activeProviders = providerRepository.count();

        Map<String, Object> res = new HashMap<>();
        res.put("revenue", totalRevenue);
        res.put("activeBookings", activeBookings);
        res.put("pendingPayout", pendingPayout);
        res.put("activeProviders", activeProviders);

        return res;
    }
    @GetMapping("/monthly-revenue")
    public List<Map<String, Object>> monthlyRevenue() {

        List<ProviderEarning> paid =
                earningRepository.findByStatus(EarningStatus.PAID);

        Map<String, Double> monthlyMap = new HashMap<>();

        for (ProviderEarning e : paid) {
            if (e.getCreatedAt() != null) {
                Month month = e.getCreatedAt().getMonth();
                monthlyMap.put(
                        month.toString(),
                        monthlyMap.getOrDefault(month.toString(), 0.0) + e.getAmount()
                );
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();

        monthlyMap.forEach((month, amount) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("month", month.substring(0,3));
            row.put("amount", amount);
            result.add(row);
        });

        return result;
    }
    @GetMapping("/service-distribution")
    public List<Map<String, Object>> serviceDistribution() {

        List<Booking> completed =
                bookingRepository.findByStatus(BookingStatus.COMPLETED);

        Map<ServiceType, Integer> countMap = new HashMap<>();

        for (Booking b : completed) {
            for (ServiceType s : b.getServices()) {
                countMap.put(s, countMap.getOrDefault(s, 0) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();

        countMap.forEach((service, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("name", service.name());
            row.put("value", count);
            result.add(row);
        });

        return result;
    }

}
