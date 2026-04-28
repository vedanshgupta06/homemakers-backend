package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final BookingRepository bookingRepository;
    private final ProviderRepository providerRepository;
    private final ProviderEarningRepository earningRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public AdminAnalyticsController(
            BookingRepository bookingRepository,
            ProviderRepository providerRepository,
            ProviderEarningRepository earningRepository,
            PaymentTransactionRepository paymentTransactionRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.providerRepository = providerRepository;
        this.earningRepository = earningRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    // ── SUMMARY ──────────────────────────────────────────────
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        double totalRevenue = earningRepository
                .findByStatus(EarningStatus.PAID)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        // ✅ Count CONFIRMED + SERVICE_IN_PROGRESS
        long activeBookings = bookingRepository.findAll()
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.SERVICE_IN_PROGRESS)
                .count();

        double pendingPayout = earningRepository
                .findByStatus(EarningStatus.AVAILABLE)
                .stream()
                .mapToDouble(ProviderEarning::getAmount)
                .sum();

        long totalProviders = providerRepository.count();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("revenue",         totalRevenue);
        res.put("activeBookings",  activeBookings);
        res.put("pendingPayout",   pendingPayout);
        res.put("activeProviders", totalProviders);
        return res;
    }

    // ── MONTHLY REVENUE ───────────────────────────────────────
    @GetMapping("/monthly-revenue")
    public List<Map<String, Object>> monthlyRevenue() {

        List<ProviderEarning> paid = earningRepository.findByStatus(EarningStatus.PAID);

        // ✅ Key by "YYYY-MM" to correctly separate same months across years
        Map<String, Double> monthlyMap = new TreeMap<>();

        String[] monthNames = {"Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"};

        for (ProviderEarning e : paid) {
            if (e.getCreatedAt() != null) {
                int year  = e.getCreatedAt().getYear();
                int month = e.getCreatedAt().getMonthValue();
                String key = String.format("%04d-%02d", year, month);
                monthlyMap.put(key, monthlyMap.getOrDefault(key, 0.0) + e.getAmount());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        monthlyMap.forEach((key, amount) -> {
            int monthNum = Integer.parseInt(key.substring(5)); // extract MM
            int year     = Integer.parseInt(key.substring(0, 4));
            Map<String, Object> row = new LinkedHashMap<>();
            // Show "Apr '26" style for clarity when multi-year
            row.put("month", monthNames[monthNum - 1] + " '" + String.valueOf(year).substring(2));
            row.put("amount", Math.round(amount * 100.0) / 100.0);
            result.add(row);
        });

        return result;
    }

    // ── SERVICE DISTRIBUTION ──────────────────────────────────
    @GetMapping("/service-distribution")
    public List<Map<String, Object>> serviceDistribution() {

        // ✅ Exclude ALL terminal/cancelled statuses
        List<Booking> bookings = bookingRepository.findAll()
                .stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                        && b.getStatus() != BookingStatus.CANCELLED
                        && b.getStatus() != BookingStatus.TERMINATED)
                .toList();

        Map<String, Integer> countMap = new LinkedHashMap<>();

        for (Booking b : bookings) {
            if (b.getServices() != null) {
                for (ServiceType s : b.getServices()) {
                    // ✅ Format nicely: DISH_WASHING → Dish Washing
                    String formatted = Arrays.stream(s.name().split("_"))
                            .map(w -> w.charAt(0) + w.substring(1).toLowerCase())
                            .reduce("", (a, c) -> a.isEmpty() ? c : a + " " + c);
                    countMap.put(formatted, countMap.getOrDefault(formatted, 0) + 1);
                }
            }
        }

        // Sort by count descending
        List<Map<String, Object>> result = new ArrayList<>();
        countMap.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name",  entry.getKey());
                    row.put("value", entry.getValue());
                    result.add(row);
                });

        return result;
    }

    // ── BOOKING REVENUE by period ─────────────────────────────
    @GetMapping("/booking-revenue")
    public Map<String, Object> getBookingRevenue(
            @RequestParam(defaultValue = "ALL") String period
    ) {
        LocalDate now  = LocalDate.now();
        LocalDate from = switch (period) {
            case "DAY"     -> now;
            case "MONTH"   -> now.withDayOfMonth(1);
            case "QUARTER" -> {
                int m = now.getMonthValue();
                int quarterStart = ((m - 1) / 3) * 3 + 1;
                yield now.withMonth(quarterStart).withDayOfMonth(1);
            }
            case "YEAR"    -> now.withDayOfMonth(1).withMonth(1);
            default        -> null; // ALL
        };

        List<PaymentTransaction> transactions = paymentTransactionRepository.findAll()
                .stream()
                // ✅ Only PAID transactions
                .filter(t -> t.getStatus() == PaymentStatus.PAID)
                // ✅ Only booking-linked transactions
                .filter(t -> t.getBookingId() != null)
                // ✅ Date filter — skip if createdAt is null (treat as excluded)
                .filter(t -> {
                    if (from == null) return true;
                    if (t.getCreatedAt() == null) return false;
                    LocalDate txnDate = t.getCreatedAt().toLocalDate();
                    // ✅ DAY: exact match; others: from <= date <= now
                    return period.equals("DAY")
                            ? txnDate.isEqual(now)
                            : (!txnDate.isBefore(from) && !txnDate.isAfter(now));
                })
                .toList();

        double stripe   = transactions.stream()
                .filter(t -> t.getMethod() == PaymentMethod.STRIPE)
                .mapToDouble(PaymentTransaction::getAmount).sum();

        double razorpay = transactions.stream()
                .filter(t -> t.getMethod() == PaymentMethod.RAZORPAY)
                .mapToDouble(PaymentTransaction::getAmount).sum();

        double wallet   = transactions.stream()
                .filter(t -> t.getMethod() == PaymentMethod.WALLET)
                .mapToDouble(PaymentTransaction::getAmount).sum();

        double total    = stripe + razorpay + wallet;

        long count = transactions.stream()
                .map(PaymentTransaction::getBookingId)
                .distinct().count();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalCollected",    Math.round(total    * 100.0) / 100.0);
        res.put("stripeCollected",   Math.round(stripe   * 100.0) / 100.0);
        res.put("razorpayCollected", Math.round(razorpay * 100.0) / 100.0);
        res.put("walletUsedTotal",   Math.round(wallet   * 100.0) / 100.0);
        res.put("paidBookingsCount", count);
        res.put("period",            period);
        return res;
    }
}