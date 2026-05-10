package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderWorkLogRepository;
import com.homemakers.homemakers.repository.UserWalletTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProviderSettlementService {

    private final ProviderWorkLogRepository workLogRepository;
    private final ProviderEarningService earningService;
    private final UserWalletService userWalletService;
    private final UserWalletTransactionRepository walletTxnRepository;
    private final ProviderNotificationService notificationService;
    public ProviderSettlementService(
            ProviderWorkLogRepository workLogRepository,
            ProviderEarningService earningService,
            UserWalletService userWalletService,
            UserWalletTransactionRepository walletTxnRepository,
            ProviderNotificationService notificationService
    ) {
        this.workLogRepository = workLogRepository;
        this.earningService = earningService;
        this.userWalletService = userWalletService;
        this.walletTxnRepository = walletTxnRepository;
        this.notificationService = notificationService;
    }

    public void finalizeSettlement(Booking booking) {

        // ── Guard: already settled ────────────────────────────
        if (earningService.existsBonusForBooking(booking.getId())) return;

        Provider provider = booking.getProvider();

        List<ProviderWorkLog> logs = workLogRepository
                .findByProviderAndBooking(provider, booking);

        // Only count logs that are not REJECTED
        List<ProviderWorkLog> activeLogs = logs.stream()
                .filter(l -> l.getStatus() != WorkStatus.REJECTED)
                .toList();

        int totalDays      = activeLogs.size();
        int presentDays    = (int) activeLogs.stream()
                .filter(l -> l.getStatus() == WorkStatus.CONFIRMED_PRESENT)
                .count();
        int leaveDays      = (int) activeLogs.stream()
                .filter(l -> l.getStatus() == WorkStatus.LEAVE)
                .count();
        int absentDays     = totalDays - presentDays - leaveDays;

        // ── Allowed paid leaves based on booking outcome ──────
        int allowedLeaves = switch (booking.getStatus()) {
            case COMPLETED   -> 3;
            case TERMINATED  -> (totalDays + 7) / 10; // ~1 per 10 days
            default          -> 0;
        };

        // Leaves within allowance are paid; extras trigger user refund
        int paidLeaves  = Math.min(leaveDays, allowedLeaves);
        int extraLeaves = Math.max(0, leaveDays - allowedLeaves);
        // Absent days always trigger refund (provider not paid for those)
        int refundDays  = absentDays + extraLeaves;

        double dailyRate = booking.getTotalPrice() / 30.0;

        // ── Provider bonus for paid leave days ────────────────
        double bonus = paidLeaves * dailyRate;
        earningService.addBonusEarning(provider, booking, bonus, LocalDate.now());

        // ── User refund for absent + extra leave days ─────────
        if (refundDays > 0) {
            boolean alreadyRefunded = walletTxnRepository
                    .existsByBookingIdAndType(booking.getId(), TransactionType.REFUND);

            if (!alreadyRefunded) {
                double refundAmount = refundDays * dailyRate;
                userWalletService.addRefund(
                        booking.getUser().getId(),
                        booking.getId(),
                        refundAmount,
                        "Refund for " + absentDays + " absent + " +
                                extraLeaves + " excess leave days on booking #" + booking.getId()
                );
            }
        }
        notificationService.settlementCompleted(
                provider,
                booking.getId(),
                presentDays,
                paidLeaves,
                bonus
        );
        // ── Mark settled on booking ───────────────────────────
        booking.setSettlementDone(true);
    }
}