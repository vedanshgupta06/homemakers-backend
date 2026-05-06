package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class BookingExpiryService {

    private final BookingRepository bookingRepository;
    private final UserWalletService userWalletService;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final ProviderPenaltyLedgerRepository penaltyLedgerRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProviderEarningService earningService;
    private final ProviderNotificationService notificationService;

    private static final double PENALTY_AMOUNT = 200.0;

    public BookingExpiryService(
            BookingRepository bookingRepository,
            UserWalletService userWalletService,
            ProviderAvailabilityRepository availabilityRepository,
            ProviderPenaltyLedgerRepository penaltyLedgerRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            ProviderEarningService earningService,
            ProviderNotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.userWalletService = userWalletService;
        this.availabilityRepository = availabilityRepository;
        this.penaltyLedgerRepository = penaltyLedgerRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.earningService = earningService;
        this.notificationService = notificationService;
    }

    // =========================================================
    // Job 1 — Cancel/Penalty runner (every 15 minutes)
    // ✅ FIXED: Now checks full DateTime (date + booking start
    //    time) so a 10 AM slot is caught at 10 AM, not next day
    // =========================================================
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void expireOldBookings() {

        // ── Cancel PENDING bookings older than 2 days ──
        LocalDateTime expiryTime = LocalDateTime.now().minusDays(2);

        List<Booking> expiredPending =
                bookingRepository.findByStatusAndCreatedAtBefore(
                        BookingStatus.PENDING, expiryTime
                );

        for (Booking booking : expiredPending) {
            cancelAndRefund(booking, false);
        }

        // ── Cancel CONFIRMED bookings whose scheduled date+time has passed ──
        LocalDateTime now = LocalDateTime.now();

        List<Booking> confirmedBookings =
                bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        for (Booking booking : confirmedBookings) {
            LocalDate scheduledDate = booking.getAvailability().getDate();
            LocalTime scheduledTime = booking.getBookingStartTime();

            // ✅ Use full DateTime — fall back to midnight if no time set
            LocalDateTime scheduledDateTime = (scheduledTime != null)
                    ? LocalDateTime.of(scheduledDate, scheduledTime)
                    : LocalDateTime.of(scheduledDate, LocalTime.MIDNIGHT);

            if (scheduledDateTime.isBefore(now)) {
                System.out.println("⏰ Booking #" + booking.getId()
                        + " scheduled for " + scheduledDateTime + " has passed. Cancelling...");
                cancelAndRefund(booking, true);
            }
        }

        // ── Safety net — catch CANCELLED bookings with still-PAID Stripe txns ──
        List<Booking> cancelledBookings =
                bookingRepository.findByStatus(BookingStatus.CANCELLED);

        for (Booking booking : cancelledBookings) {

            List<PaymentTransaction> txns =
                    paymentTransactionRepository.findAllByBookingId(booking.getId());

            boolean hasMissedRefund = txns.stream()
                    .anyMatch(t -> t.getMethod() == PaymentMethod.STRIPE
                            && t.getStatus() == PaymentStatus.PAID);

            if (hasMissedRefund) {
                System.out.println("⚠️ Found missed refund for cancelled booking #" + booking.getId());

                for (PaymentTransaction txn : txns) {
                    if (txn.getMethod() == PaymentMethod.STRIPE
                            && txn.getStatus() == PaymentStatus.PAID) {

                        userWalletService.addRefund(
                                booking.getUser().getId(),
                                booking.getId(),
                                txn.getAmount(),
                                "Refund ₹" + (int) txn.getAmount() +
                                        " — Booking #" + booking.getId() +
                                        " was cancelled. Amount added to your wallet."
                        );

                        txn.setStatus(PaymentStatus.REFUNDED);
                        txn.setDescription("REFUNDED — " + txn.getDescription());
                        paymentTransactionRepository.save(txn);

                        System.out.println("✅ Safety-net refund ₹" + txn.getAmount()
                                + " applied for booking #" + booking.getId());
                    }
                }
            }
        }
    }

    // =========================================================
    // Job 2 — Time-aware reminder sender (every 15 minutes)
    // ✅ Sends reminders relative to actual booking start time:
    //    - 60–75 min before  → "Start soon" reminder
    //    - 15–30 min before  → Final warning
    //
    // Example: slot is 6 May 10:00 AM
    //   → Reminder fires between 8:45–9:00 AM
    //   → Final warning fires between 9:30–9:45 AM
    //
    // This correctly handles late accepts too — if provider
    // accepts at 9:50 AM for a 10:00 AM slot, they immediately
    // get the final warning on next scheduler tick.
    // =========================================================
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void sendTimeAwareReminders() {

        LocalDateTime now = LocalDateTime.now();

        List<Booking> confirmed = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        for (Booking booking : confirmed) {

            // Only notify for PAID bookings
            if (booking.getPaymentStatus() != PaymentStatus.PAID) continue;

            LocalDate scheduledDate = booking.getAvailability().getDate();
            LocalTime scheduledTime = booking.getBookingStartTime();

            if (scheduledTime == null) continue;

            LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);

            // Skip if already past
            if (scheduledDateTime.isBefore(now)) continue;

            long minutesUntilStart = java.time.Duration.between(now, scheduledDateTime).toMinutes();

            String displayDateTime = scheduledDate.toString() + " at " + formatTime(scheduledTime);

            // ── 1-hour reminder: 60–75 minutes before start ──
            if (minutesUntilStart <= 75 && minutesUntilStart > 45) {
                notificationService.startDateReminder(
                        booking.getProvider(),
                        booking.getId(),
                        displayDateTime
                );
                System.out.println("🔔 1-hour reminder sent for booking #" + booking.getId()
                        + " (starts at " + scheduledDateTime + ")");
            }

            // ── Final warning: 15–30 minutes before start ──
            if (minutesUntilStart <= 30 && minutesUntilStart > 0) {
                notificationService.startDateFinalWarning(
                        booking.getProvider(),
                        booking.getId(),
                        displayDateTime
                );
                System.out.println("🚨 Final warning sent for booking #" + booking.getId()
                        + " (starts at " + scheduledDateTime + ")");
            }
        }
    }

    // =========================================================
    // cancelAndRefund
    // =========================================================
    private void cancelAndRefund(Booking booking, boolean applyPenalty) {

        System.out.println("🔄 Processing booking #" + booking.getId()
                + " | status=" + booking.getStatus()
                + " | paymentStatus=" + booking.getPaymentStatus()
                + " | walletUsed=" + booking.getWalletUsed()
                + " | finalPayable=" + booking.getFinalPayableAmount());

        // ── Step 1: Resolve Stripe transactions ──
        List<PaymentTransaction> txns =
                paymentTransactionRepository.findAllByBookingId(booking.getId());

        double stripeRefundTotal = txns.stream()
                .filter(t -> t.getMethod() == PaymentMethod.STRIPE
                        && t.getStatus() == PaymentStatus.PAID)
                .mapToDouble(PaymentTransaction::getAmount)
                .sum();

        // ── Step 2: Wallet handling ──
        double walletUsed = booking.getWalletUsed() != null ? booking.getWalletUsed() : 0.0;

        if (walletUsed > 0) {

            if (booking.getStatus() == BookingStatus.PENDING) {
                userWalletService.releaseReservedAmount(
                        booking.getUser().getId(),
                        booking.getId(),
                        walletUsed
                );
                System.out.println("✅ Released wallet reservation: ₹" + walletUsed);

            } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
                double walletRefundAmount = Math.max(0, walletUsed - stripeRefundTotal);

                if (walletRefundAmount > 0) {
                    userWalletService.refundToWallet(
                            booking.getUser().getId(),
                            walletRefundAmount,
                            "Wallet refund ₹" + (int) walletRefundAmount +
                                    " — Booking #" + booking.getId() +
                                    " cancelled (provider did not show up on scheduled time)"
                    );
                    System.out.println("✅ Refunded wallet portion: ₹" + walletRefundAmount);
                } else {
                    System.out.println("⏭ Skipping wallet refund for booking #" + booking.getId()
                            + " — Stripe refund already covers the full amount");
                }
            }
        }

        // ── Step 3: Stripe payment refund → user wallet ──
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {

            System.out.println("🔍 Found " + txns.size() + " payment txns for booking #" + booking.getId());

            for (PaymentTransaction txn : txns) {
                if (txn.getMethod() == PaymentMethod.STRIPE
                        && txn.getStatus() == PaymentStatus.PAID) {

                    String scheduledAt = booking.getAvailability().getDate().toString()
                            + (booking.getBookingStartTime() != null
                            ? " at " + formatTime(booking.getBookingStartTime()) : "");

                    String refundReason = applyPenalty
                            ? "Refund ₹" + (int) txn.getAmount() +
                            " — Booking #" + booking.getId() +
                            " cancelled. Provider confirmed for " + scheduledAt +
                            " but did not show up. Money added to your wallet."
                            : "Refund ₹" + (int) txn.getAmount() +
                            " — Booking #" + booking.getId() +
                            " cancelled. Provider did not accept within 2 days." +
                            " Money added to your wallet.";

                    userWalletService.addRefund(
                            booking.getUser().getId(),
                            booking.getId(),
                            txn.getAmount(),
                            refundReason
                    );

                    txn.setStatus(PaymentStatus.REFUNDED);
                    txn.setDescription("REFUNDED — " + txn.getDescription());
                    paymentTransactionRepository.save(txn);

                    System.out.println("✅ Refunded Stripe payment ₹" + txn.getAmount()
                            + " to wallet for user #" + booking.getUser().getId());
                }
            }
        }

        // ── Step 4: Provider penalty (only CONFIRMED + PAID) ──
        if (applyPenalty && booking.getStatus() == BookingStatus.CONFIRMED) {
            applyProviderPenalty(booking);
        }

        // ── Step 5: Cancel the booking ──
        booking.setStatus(BookingStatus.CANCELLED);

        // ── Step 6: Free the provider slot ──
        ProviderAvailability slot = booking.getAvailability();
        slot.setActive(true);
        availabilityRepository.save(slot);

        bookingRepository.save(booking);
        System.out.println("✅ Booking #" + booking.getId() + " cancelled successfully");
    }

    // =========================================================
    // applyProviderPenalty
    // =========================================================
    private void applyProviderPenalty(Booking booking) {

        // ✅ No penalty if user never paid — not the provider's fault
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            System.out.println("⏭ Skipping penalty for booking #" + booking.getId() + " — not paid");
            return;
        }

        boolean alreadyPenalised = penaltyLedgerRepository.existsByBooking(booking);

        if (alreadyPenalised) {
            System.out.println("⏭ Penalty already applied for booking #" + booking.getId());
            return;
        }

        Provider provider    = booking.getProvider();
        String scheduledDate = booking.getAvailability().getDate().toString();
        String scheduledTime = booking.getBookingStartTime() != null
                ? " at " + formatTime(booking.getBookingStartTime()) : "";

        String reason = "Penalty ₹" + (int) PENALTY_AMOUNT +
                " — confirmed booking #" + booking.getId() +
                " for " + scheduledDate + scheduledTime +
                " but did not start work. Booking cancelled and user refunded.";

        ProviderPenaltyLedger penalty = new ProviderPenaltyLedger();
        penalty.setProvider(provider);
        penalty.setBooking(booking);
        penalty.setPenaltyAmount(PENALTY_AMOUNT);
        penalty.setReason(reason);
        penalty.setCreatedAt(LocalDateTime.now());
        penalty.setDeducted(true);
        penaltyLedgerRepository.save(penalty);

        earningService.addPenaltyEarning(provider, booking, PENALTY_AMOUNT, reason);

        booking.setPenaltyApplied(PENALTY_AMOUNT);

        // ✅ Notify provider about cancellation + penalty
        notificationService.bookingAutoCancelledWithPenalty(
                provider,
                booking.getId(),
                scheduledDate + scheduledTime,
                PENALTY_AMOUNT
        );

        System.out.println("✅ Penalty ₹" + PENALTY_AMOUNT
                + " applied to provider #" + provider.getId());
    }

    // =========================================================
    // Helper — formats LocalTime to "10:00 AM" style
    // =========================================================
    private String formatTime(LocalTime time) {
        if (time == null) return "";
        int hour        = time.getHour();
        int minute      = time.getMinute();
        String amPm     = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%d:%02d %s", displayHour, minute, amPm);
    }
}