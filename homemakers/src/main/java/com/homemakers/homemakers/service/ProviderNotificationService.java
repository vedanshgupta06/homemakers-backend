package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.ProviderNotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class ProviderNotificationService {

    private final ProviderNotificationRepository notificationRepository;

    public ProviderNotificationService(ProviderNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void send(Provider provider, NotificationType type, String title, String message) {
        ProviderNotification notif = new ProviderNotification();
        notif.setProvider(provider);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notificationRepository.save(notif);
    }

    // ── Booking events ────────────────────────────────────────

    public void newBookingRequest(Provider provider, Long bookingId, String customerName, String services) {
        send(provider, NotificationType.BOOKING,
                "New Booking Request 🔔",
                "You have a new booking request #" + bookingId + " from " + customerName +
                        " for " + services + ". Please accept or reject it.");
    }

    public void bookingCancelled(Provider provider, Long bookingId, String customerName) {
        send(provider, NotificationType.BOOKING,
                "Booking Cancelled ❌",
                "Booking #" + bookingId + " from " + customerName + " has been cancelled.");
    }

    public void bookingPaymentReceived(Provider provider, Long bookingId, double amount) {
        send(provider, NotificationType.BOOKING,
                "Payment Received ✅",
                "Payment of ₹" + String.format("%.0f", amount) +
                        " received for booking #" + bookingId + ". You can now start work on the scheduled date.");
    }

    // ── Attendance events ─────────────────────────────────────

    public void attendanceConfirmed(Provider provider, Long bookingId, String date) {
        send(provider, NotificationType.GENERAL,
                "Attendance Confirmed ✅",
                "Your attendance for booking #" + bookingId + " on " + date +
                        " has been confirmed by the customer. Your earning has been credited.");
    }

    public void attendanceRejected(Provider provider, Long bookingId, String date) {
        send(provider, NotificationType.GENERAL,
                "Attendance Rejected ⚠️",
                "Your attendance for booking #" + bookingId + " on " + date +
                        " was rejected by the customer. Please contact support if this is incorrect.");
    }

    // ── Payout events ─────────────────────────────────────────

    public void payoutRequested(Provider provider, double amount) {
        send(provider, NotificationType.PAYOUT,
                "Payout Request Submitted 📤",
                "Your payout request of ₹" + String.format("%.0f", amount) +
                        " has been submitted and is pending admin approval.");
    }

    public void payoutApproved(Provider provider, double amount) {
        send(provider, NotificationType.PAYOUT,
                "Payout Approved ✅",
                "Your payout of ₹" + String.format("%.0f", amount) +
                        " has been approved and will be transferred to your account shortly.");
    }

    public void payoutPaid(Provider provider, double amount) {
        send(provider, NotificationType.PAYOUT,
                "Payout Transferred 💰",
                "₹" + String.format("%.0f", amount) +
                        " has been successfully transferred to your account.");
    }

    // ── Settlement events ─────────────────────────────────────

    public void settlementCompleted(Provider provider, Long bookingId,
                                    int presentDays, int paidLeaves, double bonus) {
        send(provider, NotificationType.PAYOUT,
                "Booking Settlement Completed 📊",
                "Booking #" + bookingId + " has been settled. Present: " + presentDays +
                        " days, Paid leaves: " + paidLeaves + " days" +
                        (bonus > 0 ? ", Bonus credited: ₹" + String.format("%.0f", bonus) : "") + ".");
    }

    // ── Start date reminder events ────────────────────────────

    /**
     * Sent at 9 AM on the scheduled start date.
     * Reminds the provider to start work today to avoid penalty.
     */
    public void startDateReminder(Provider provider, Long bookingId, String scheduledDate) {
        send(provider, NotificationType.REMINDER,
                "⏰ Starting in 2 Hours — Booking #" + bookingId,
                "Your booking #" + bookingId + " starts at " + scheduledDate + ". " +
                        "Please tap 'Start Work' when you arrive on time to avoid a ₹200 penalty.");
    }

    public void startDateFinalWarning(Provider provider, Long bookingId, String scheduledDate) {
        send(provider, NotificationType.REMINDER,
                "🚨 30 Minutes Left — Booking #" + bookingId,
                "Booking #" + bookingId + " starts at " + scheduledDate + ". " +
                        "Tap 'Start Work' now or it will be auto-cancelled with a ₹200 penalty deducted.");
    }
    /**
     * Sent when the booking is auto-cancelled due to provider no-show.
     * Notifies the provider that penalty has been applied.
     */
    public void bookingAutoCancelledWithPenalty(Provider provider, Long bookingId,
                                                String scheduledDate, double penaltyAmount) {
        send(provider, NotificationType.BOOKING,
                "Booking Auto-Cancelled ❌ — Penalty Applied",
                "Booking #" + bookingId + " scheduled for " + scheduledDate +
                        " was auto-cancelled because work was not started on time. " +
                        "A penalty of ₹" + String.format("%.0f", penaltyAmount) +
                        " has been deducted from your earnings. The customer has been fully refunded.");
    }
}