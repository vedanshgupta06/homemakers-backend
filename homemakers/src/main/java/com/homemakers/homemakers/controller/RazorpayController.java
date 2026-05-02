package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.PaymentTransactionRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.PaymentService;
import com.homemakers.homemakers.service.UserWalletService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")
public class RazorpayController {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentService paymentService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserWalletService userWalletService;
    @Autowired private PaymentTransactionRepository paymentTransactionRepository;

    // ── CREATE ORDER ──────────────────────────────────────────
    @PostMapping("/order/{bookingId}")
    public ResponseEntity<?> createOrder(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (booking.getFinalPayableAmount() == null || booking.getFinalPayableAmount() <= 0) {
                return ResponseEntity.badRequest().body("No amount due for this booking");
            }

            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            options.put("amount", (int)(booking.getFinalPayableAmount() * 100)); // paise
            options.put("currency", "INR");
            options.put("receipt", "booking_" + bookingId);

            // Store bookingId in notes so verify can retrieve it
            JSONObject notes = new JSONObject();
            notes.put("bookingId", bookingId.toString());
            options.put("notes", notes);

            com.razorpay.Order order = client.orders.create(options);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.status(500).body("Razorpay error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to create order: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> payload) {
        try {
            String orderId   = payload.get("razorpay_order_id");
            String paymentId = payload.get("razorpay_payment_id");
            String signature = payload.get("razorpay_signature");

            // 1. Verify HMAC signature
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            if (!hexString.toString().equals(signature)) {
                return ResponseEntity.status(400).body("Invalid payment signature");
            }

            // 2. Get bookingId from Razorpay order notes
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            com.razorpay.Order order = client.orders.fetch(orderId);
            JSONObject notes = order.get("notes");
            Long bookingId = Long.parseLong(notes.getString("bookingId"));

            // 3. Fetch booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // 4. Guard — already paid
            if (booking.getPaymentStatus() == PaymentStatus.PAID) {
                return ResponseEntity.ok(Map.of("message", "Already paid"));
            }

            // 5. Record amount BEFORE marking paid (same pattern as your Stripe flow)
            double amount = booking.getFinalPayableAmount();

            // 6. Save payment transaction
            PaymentTransaction txn = new PaymentTransaction();
            txn.setUserId(booking.getUser().getId());
            txn.setBookingId(bookingId);
            txn.setAmount(amount);
            txn.setMethod(PaymentMethod.RAZORPAY);
            txn.setStatus(PaymentStatus.PAID);
            txn.setDescription("Booking payment via Razorpay");
            paymentTransactionRepository.save(txn);

            // 7. Mark booking as paid — uses your existing PaymentService method
            // This sets paymentStatus=PAID, status=CONFIRMED and saves
            paymentService.markBookingAsPaid(booking);

            // 8. Clear the due amount
            booking.setFinalPayableAmount(0.0);
            bookingRepository.save(booking);

            System.out.println("✅ Razorpay payment verified for booking: " + bookingId);

            return ResponseEntity.ok(Map.of("message", "Payment verified successfully"));

        } catch (Exception e) {
            e.printStackTrace(); // ← shows exact error in Spring Boot console
            return ResponseEntity.status(500).body("Verification failed: " + e.getMessage());
        }
    }
    // ── CREATE WALLET RECHARGE ORDER ──────────────────────────
    @PostMapping("/wallet/order/{amount}")
    public ResponseEntity<?> createWalletOrder(@PathVariable double amount) {
        try {
            // Get email from SecurityContext — same as your other controllers
            String email = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();

            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            options.put("amount", (int)(amount * 100));
            options.put("currency", "INR");
            options.put("receipt", "wallet_" + System.currentTimeMillis());

            JSONObject notes = new JSONObject();
            notes.put("type", "WALLET_RECHARGE");
            notes.put("userEmail", email);
            options.put("notes", notes);

            com.razorpay.Order order = client.orders.create(options);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed: " + e.getMessage());
        }
    }

    // ── VERIFY WALLET RECHARGE ────────────────────────────────
    @PostMapping("/wallet/verify")
    public ResponseEntity<?> verifyWalletPayment(@RequestBody Map<String, String> payload) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();

            String orderId   = payload.get("razorpay_order_id");
            String paymentId = payload.get("razorpay_payment_id");
            String signature = payload.get("razorpay_signature");

            // Verify signature
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            if (!hex.toString().equals(signature)) {
                return ResponseEntity.status(400).body("Invalid signature");
            }

            // Get amount from Razorpay order
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            com.razorpay.Order order = client.orders.fetch(orderId);
            double amount = ((Number) order.get("amount")).doubleValue() / 100.0;

            // Add to wallet
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            userWalletService.addMoney(user.getId(), amount);

            // Record transaction
            PaymentTransaction txn = new PaymentTransaction();
            txn.setUserId(user.getId());
            txn.setAmount(amount);
            txn.setMethod(PaymentMethod.RAZORPAY);
            txn.setStatus(PaymentStatus.PAID);
            txn.setDescription("Wallet recharge via Razorpay");
            paymentTransactionRepository.save(txn);

            return ResponseEntity.ok(Map.of("message", "Wallet recharged successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Verification failed: " + e.getMessage());
        }
    }
}
