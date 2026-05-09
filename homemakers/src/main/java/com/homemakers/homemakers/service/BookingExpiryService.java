package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    // =========================================================
    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional
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
    // =========================================================
    private final Set<String> sentReminders = new HashSet<>();
    @Transactional
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void sendTimeAwareReminders() {

        LocalDateTime now = LocalDateTime.now();

        List<Booking> confirmed = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        for (Booking booking : confirmed) {

            if (booking.getPaymentStatus() != PaymentStatus.PAID) continue;

            LocalDate scheduledDate = booking.getAvailability().getDate();
            LocalTime scheduledTime = booking.getBookingStartTime();

            if (scheduledTime == null) continue;

            LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);

            if (scheduledDateTime.isBefore(now)) continue;

            long minutesUntilStart = java.time.Duration.between(now, scheduledDateTime).toMinutes();

            String displayDateTime = scheduledDate + " at " + formatTime(scheduledTime);

            // 2-hour reminder: 105–135 minutes before start
            String morningKey = "morning-" + booking.getId() + "-" + scheduledDate;
            if (minutesUntilStart <= 135 && minutesUntilStart > 105
                    && !sentReminders.contains(morningKey)) {
                notificationService.startDateReminder(
                        booking.getProvider(), booking.getId(), displayDateTime);
                sentReminders.add(morningKey);
                System.out.println("🔔 2-hour reminder sent for booking #" + booking.getId());
            }

            // Final warning: 15–30 minutes before start
            String finalKey = "final-" + booking.getId() + "-" + scheduledDate;
            if (minutesUntilStart <= 30 && minutesUntilStart > 15
                    && !sentReminders.contains(finalKey)) {
                notificationService.startDateFinalWarning(
                        booking.getProvider(), booking.getId(), displayDateTime);
                sentReminders.add(finalKey);
                System.out.println("🚨 Final warning sent for booking #" + booking.getId());
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

        // ── Step 6: Reactivate anchor slot + ALL future locked slots ──
        // ✅ FIXED: Previously only reactivated the anchor slot (start date).
        // Now also reactivates all future slots that were locked for the
        // 30-day range when the booking was accepted.
        reactivateAllLockedSlots(booking);

        bookingRepository.save(booking);
        System.out.println("✅ Booking #" + booking.getId() + " cancelled successfully");
    }

    // =========================================================
    // ✅ NEW — Reactivate anchor slot + all future locked slots
    // Called when booking is cancelled by the scheduler.
    // Mirrors the same logic in BookingService.reactivateSlotForBooking()
    // =========================================================
    private void reactivateAllLockedSlots(Booking booking) {
        if (booking.getAvailability() == null) return;

        Provider provider   = booking.getProvider();
        LocalDate startDate = booking.getAvailability().getDate();
        LocalTime bookStart = booking.getBookingStartTime();
        LocalTime bookEnd   = booking.getBookingEndTime();

        // Estimate 30-day range (no workEndDate since booking was cancelled, not completed)
        LocalDate endDate = startDate.plusDays(30);

        // ── Reactivate the anchor slot itself ──
        ProviderAvailability anchorSlot = booking.getAvailability();
        if (!anchorSlot.isActive()) {
            anchorSlot.setActive(true);
            availabilityRepository.save(anchorSlot);
            System.out.println("🔓 Reactivated anchor slot #" + anchorSlot.getId()
                    + " on " + anchorSlot.getDate());
        }

        if (bookStart == null || bookEnd == null) return;

        // ── Reactivate all future slots locked for this booking's time window ──
        List<ProviderAvailability> allSlots = availabilityRepository.findByProvider(provider);

        for (ProviderAvailability slot : allSlots) {

            // Only slots after the anchor date within 30-day range
            if (slot.getDate().isBefore(startDate.plusDays(1))) continue;
            if (slot.getDate().isAfter(endDate)) continue;

            // Only inactive slots
            if (slot.isActive()) continue;

            // Only slots overlapping the booking time window
            boolean overlaps = slot.getStartTime().isBefore(bookEnd)
                    && slot.getEndTime().isAfter(bookStart);

            if (overlaps) {
                // Safety check: don't reactivate if another booking uses this slot
                boolean hasOtherBooking = bookingRepository.existsByAvailability(slot);

                if (!hasOtherBooking) {
                    slot.setActive(true);
                    availabilityRepository.save(slot);
                    System.out.println("🔓 Reactivated future slot #" + slot.getId()
                            + " on " + slot.getDate()
                            + " " + slot.getStartTime() + "-" + slot.getEndTime());
                }
            }
        }
    }

    // =========================================================
    // applyProviderPenalty
    // =========================================================
    private void applyProviderPenalty(Booking booking) {

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