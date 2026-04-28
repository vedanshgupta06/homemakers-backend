package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingExpiryService {

    private final BookingRepository bookingRepository;
    private final UserWalletService userWalletService;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final ProviderPenaltyLedgerRepository penaltyLedgerRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProviderEarningService earningService;

    private static final double PENALTY_AMOUNT = 200.0;

    public BookingExpiryService(
            BookingRepository bookingRepository,
            UserWalletService userWalletService,
            ProviderAvailabilityRepository availabilityRepository,
            ProviderPenaltyLedgerRepository penaltyLedgerRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            ProviderEarningService earningService
    ) {
        this.bookingRepository = bookingRepository;
        this.userWalletService = userWalletService;
        this.availabilityRepository = availabilityRepository;
        this.penaltyLedgerRepository = penaltyLedgerRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.earningService = earningService;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void expireOldBookings() {

        // ── Job 1: Cancel PENDING bookings older than 2 days ──
        LocalDateTime expiryTime = LocalDateTime.now().minusDays(2);

        List<Booking> expiredPending =
                bookingRepository.findByStatusAndCreatedAtBefore(
                        BookingStatus.PENDING, expiryTime
                );

        for (Booking booking : expiredPending) {
            cancelAndRefund(booking, false);
        }

        // ── Job 2: Cancel CONFIRMED bookings whose scheduled date has passed ──
        LocalDate today = LocalDate.now();

        List<Booking> confirmedExpired =
                bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        for (Booking booking : confirmedExpired) {
            LocalDate scheduledDate = booking.getAvailability().getDate();
            if (scheduledDate.isBefore(today)) {
                cancelAndRefund(booking, true);
            }
        }

        // ── Job 3: Safety net — catch CANCELLED bookings with still-PAID Stripe txns ──
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

    private void cancelAndRefund(Booking booking, boolean applyPenalty) {

        System.out.println("🔄 Processing booking #" + booking.getId()
                + " | status=" + booking.getStatus()
                + " | paymentStatus=" + booking.getPaymentStatus()
                + " | walletUsed=" + booking.getWalletUsed()
                + " | finalPayable=" + booking.getFinalPayableAmount());

        // ── Step 1: Resolve Stripe transactions first ──
        // Find the total amount already being refunded via Stripe
        // so we don't double-credit any wallet portion covered by Stripe.
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
                // PENDING — wallet was only reserved, never charged. Release the hold.
                userWalletService.releaseReservedAmount(
                        booking.getUser().getId(),
                        booking.getId(),
                        walletUsed
                );
                System.out.println("✅ Released wallet reservation: ₹" + walletUsed);

            } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
                // CONFIRMED — wallet was actually debited.
                // Only refund wallet portion that is NOT already covered by the Stripe refund.
                // e.g. finalPayable=2000, stripeRefundTotal=1500, walletUsed=500 → refund 500
                // e.g. finalPayable=2000, stripeRefundTotal=2000, walletUsed=2000 → DON'T refund wallet (Stripe covers it all)
                double walletRefundAmount = Math.max(0, walletUsed - stripeRefundTotal);

                if (walletRefundAmount > 0) {
                    userWalletService.refundToWallet(
                            booking.getUser().getId(),
                            walletRefundAmount,
                            "Wallet refund ₹" + (int) walletRefundAmount +
                                    " — Booking #" + booking.getId() +
                                    " cancelled (provider did not show up on scheduled date)"
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

                    String refundReason = applyPenalty
                            ? "Refund ₹" + (int) txn.getAmount() +
                            " — Booking #" + booking.getId() +
                            " cancelled. Provider confirmed for " +
                            booking.getAvailability().getDate() +
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

        Provider provider = booking.getProvider();

        String reason = "Penalty ₹" + (int) PENALTY_AMOUNT +
                " — confirmed booking #" + booking.getId() +
                " for " + booking.getAvailability().getDate() +
                " but did not start work. Booking cancelled and user refunded.";

        ProviderPenaltyLedger penalty = new ProviderPenaltyLedger();
        penalty.setProvider(provider);
        penalty.setBooking(booking);
        penalty.setPenaltyAmount(PENALTY_AMOUNT);
        penalty.setReason(reason);
        penalty.setCreatedAt(LocalDateTime.now());
        penalty.setDeducted(true);
        penaltyLedgerRepository.save(penalty);

        earningService.addPenaltyEarning(
                provider,
                booking,
                PENALTY_AMOUNT,
                reason
        );

        booking.setPenaltyApplied(PENALTY_AMOUNT);
        System.out.println("✅ Penalty ₹" + PENALTY_AMOUNT + " applied to provider #" + provider.getId());
    }
}