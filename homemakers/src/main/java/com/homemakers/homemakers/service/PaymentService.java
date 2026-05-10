//package com.homemakers.homemakers.service;
//
//import com.homemakers.homemakers.model.*;
//import com.homemakers.homemakers.repository.BookingRepository;
//import com.homemakers.homemakers.repository.PaymentTransactionRepository;
//import com.homemakers.homemakers.repository.UserRepository;
//import com.stripe.Stripe;
//import com.stripe.model.Event;
//import com.stripe.model.StripeObject;
//import com.stripe.model.checkout.Session;
//import com.stripe.model.EventDataObjectDeserializer;
//import com.stripe.param.checkout.SessionCreateParams;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import com.stripe.model.PaymentIntent;
//import com.stripe.model.Charge;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.stripe.model.Charge;
//import com.homemakers.homemakers.model.PaymentMethod;
//import static com.homemakers.homemakers.model.BookingStatus.CONFIRMED;
//import static com.homemakers.homemakers.model.PaymentStatus.PAID;
//
//@Service
//public class PaymentService {
//
//    private final BookingRepository bookingRepository;
//    private final UserWalletService userWalletService;
//    @Value("${stripe.secret.key}")
//    private String stripeSecretKey;
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private PaymentTransactionRepository paymentTransactionRepository;
//    public PaymentService(BookingRepository bookingRepository, UserWalletService userWalletService) {
//        this.bookingRepository = bookingRepository;
//        this.userWalletService = userWalletService;
//    }
//
//    // =========================================================
//    // CREATE STRIPE CHECKOUT SESSION
//    // =========================================================
//
//    public String createCheckoutSession(Long bookingId) throws Exception {
//
//        Stripe.apiKey = stripeSecretKey;
//
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new RuntimeException("Booking not found"));
//
//        // ✅ Booking must be confirmed
//        if (booking.getStatus() != CONFIRMED) {
//            throw new RuntimeException("Booking must be confirmed before payment");
//        }
//
//        // ✅ Payment must be required
//        if (booking.getPaymentStatus() != PaymentStatus.PAYMENT_REQUIRED) {
//            throw new RuntimeException("Payment is not required for this booking");
//        }
//
//        // ✅ Safety check
//        if (booking.getFinalPayableAmount() == null) {
//            throw new RuntimeException("Invalid booking payment state");
//        }
//
//        double amountToPay = booking.getFinalPayableAmount();
//
//        // ✅ Handle full wallet payment case
//        if (amountToPay <= 0) {
//            throw new RuntimeException("No payment required. Fully paid via wallet.");
//        }
//
//        SessionCreateParams params =
//                SessionCreateParams.builder()
//                        .setMode(SessionCreateParams.Mode.PAYMENT)
//
//                        .setSuccessUrl("http://localhost:5173/user/payments?status=success")
//                        .setCancelUrl("http://localhost:5173/user/payments?status=cancel")
//
//                        .addLineItem(
//                                SessionCreateParams.LineItem.builder()
//                                        .setQuantity(1L)
//                                        .setPriceData(
//                                                SessionCreateParams.LineItem.PriceData.builder()
//                                                        .setCurrency("inr")
//                                                        // ✅ FIXED: use remaining amount only
//                                                        .setUnitAmount((long) (amountToPay * 100))
//                                                        .setProductData(
//                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
//                                                                        .setName("Homemakers Monthly Service")
//                                                                        .build()
//                                                        )
//                                                        .build()
//                                        )
//                                        .build()
//                        )
//
//                        // ✅ metadata on session
//                        .putMetadata("bookingId", booking.getId().toString())
//
//                        // ✅ metadata on payment intent
//                        .setPaymentIntentData(
//                                SessionCreateParams.PaymentIntentData.builder()
//                                        .putMetadata("bookingId", booking.getId().toString())
//                                        .build()
//                        )
//
//                        .build();
//
//        Session session = Session.create(params);
//
//        booking.setStripeSessionId(session.getId());
//        bookingRepository.save(booking);
//
//        return session.getUrl();
//    }
//
//    // =========================================================
//    // HANDLE STRIPE WEBHOOK
//    // =========================================================
////
////    public void handleStripeEvent(Event event) {
////
////        Stripe.apiKey = stripeSecretKey;
////
////        System.out.println("Stripe Event Received: " + event.getType());
////
////        // ✅ Only process charge events
////        if (!event.getType().startsWith("charge.")) {
////            System.out.println("Ignoring event: " + event.getType());
////            return;
////        }
////
////        try {
////
////            // Convert event to JSON
////            JsonObject eventJson = JsonParser.parseString(event.toJson()).getAsJsonObject();
////
////            JsonObject chargeObject =
////                    eventJson.getAsJsonObject("data").getAsJsonObject("object");
////
////            String chargeId = chargeObject.get("id").getAsString();
////
////            System.out.println("Charge ID: " + chargeId);
////
////            // Retrieve full charge from Stripe
////            Charge charge = Charge.retrieve(chargeId);
////
////            // 🔥 CHECK TYPE (VERY IMPORTANT)
////            PaymentIntent paymentIntent = PaymentIntent.retrieve(charge.getPaymentIntent());
////
////            String type = paymentIntent.getMetadata().get("type");
////            System.out.println("TYPE: " + type);
////            // =========================================================
////            // 🆕 WALLET RECHARGE FLOW
////            // =========================================================
////            if ("WALLET_RECHARGE".equals(type)) {
////
////                String userId = charge.getMetadata().get("userId");
////
////                if (userId == null) {
////                    System.out.println("userId missing in metadata");
////                    return;
////                }
////
////                Long id = Long.parseLong(userId);
////
////                double amount = charge.getAmount() / 100.0;
////
////                userWalletService.addRefund(
////                        id,
////                        null,
////                        amount,
////                        "Wallet recharge"
////                );
////
////                System.out.println("Wallet recharged for user: " + id + " Amount: " + amount);
////
////                return; // 🔥 VERY IMPORTANT (stop here)
////            }
////
////            // =========================================================
////            // 🧾 BOOKING PAYMENT FLOW
////            // =========================================================
////
////            String bookingId = charge.getMetadata().get("bookingId");
////
////            if (bookingId == null) {
////                System.out.println("bookingId missing in metadata");
////                return;
////            }
////
////            Long id = Long.parseLong(bookingId);
////
////            Booking booking = bookingRepository.findById(id)
////                    .orElseThrow(() -> new RuntimeException("Booking not found"));
////
////            // ✅ Idempotency check
////            if (booking.getPaymentStatus() == PaymentStatus.PAID) {
////                System.out.println("Booking already paid");
////                return;
////            }
////
////            booking.setPaymentStatus(PaymentStatus.PAID);
////            booking.setStripePaymentIntent(charge.getPaymentIntent());
////
////            bookingRepository.save(booking);
////
////            System.out.println("Booking " + id + " marked as PAID");
////
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////    public void handleStripeEvent(Event event) {
////
////        Stripe.apiKey = stripeSecretKey;
////
////        System.out.println("🔥 EVENT TYPE: " + event.getType());
////
////        if ("checkout.session.completed".equals(event.getType())) {
////
////            var optionalObj = event.getDataObjectDeserializer().getObject();
////
////            if (optionalObj.isEmpty()) {
////                System.out.println("❌ Session deserialization failed");
////                return;
////            }
////
////            Session session = (Session) optionalObj.get();
////
////            String type = session.getMetadata() != null
////                    ? session.getMetadata().get("type")
////                    : null;
////
////            // =========================
////            // 💰 WALLET RECHARGE
////            // =========================
////            if ("WALLET_RECHARGE".equals(type)) {
////
////                String userId = session.getMetadata().get("userId");
////
////                if (userId == null) {
////                    System.out.println("❌ userId missing");
////                    return;
////                }
////
////                double amount = session.getAmountTotal() / 100.0;
////
////                userWalletService.addMoney(Long.valueOf(userId), amount);
////
////                System.out.println("✅ WALLET UPDATED SUCCESSFULLY");
////                return;
////            }
////
////            // =========================
////            // 🧾 BOOKING PAYMENT
////            // =========================
////            String bookingId = session.getMetadata().get("bookingId");
////
////            if (bookingId != null) {
////
////                Booking booking = bookingRepository.findById(Long.valueOf(bookingId))
////                        .orElseThrow(() -> new RuntimeException("Booking not found"));
////
////                if (booking.getPaymentStatus() == PAID) {
////                    System.out.println("Already paid");
////                    return;
////                }
////
////                booking.setStripePaymentIntent(session.getPaymentIntent());
////
////                markBookingAsPaid(booking);
////
////                System.out.println("🔥 WEBHOOK HIT for booking: " + bookingId);
////                System.out.println("✅ BOOKING PAID");
////            }
////        }
////    }
//
//    public void handleStripeEvent(Event event) {
//
//        Stripe.apiKey = stripeSecretKey;
//
//        System.out.println("🔥 EVENT TYPE: " + event.getType());
//
//        // =====================================================
//        // ✅ 1. CHECKOUT SESSION (PRIMARY FLOW)
//        // =====================================================
//        if ("checkout.session.completed".equals(event.getType())) {
//
//            var optionalObj = event.getDataObjectDeserializer().getObject();
//
//            if (optionalObj.isEmpty()) {
//                System.out.println("❌ Session deserialization failed");
//                return;
//            }
//
//            Session session = (Session) optionalObj.get();
//
//            String type = session.getMetadata() != null
//                    ? session.getMetadata().get("type")
//                    : null;
//
//            // =========================
//            // 💰 WALLET RECHARGE
//            // =========================
//            if ("WALLET_RECHARGE".equals(type)) {
//
//                String userId = session.getMetadata().get("userId");
//
//                if (userId == null) {
//                    System.out.println("❌ userId missing");
//                    return;
//                }
//
//                double amount = session.getAmountTotal() / 100.0;
//
//                userWalletService.addMoney(Long.valueOf(userId), amount);
//
//                PaymentTransaction txn = new PaymentTransaction();
//                txn.setUserId(Long.valueOf(userId));
//                txn.setAmount(amount);
//                txn.setMethod(PaymentMethod.STRIPE);
//                txn.setStatus(PaymentStatus.PAID);
//                txn.setDescription("Wallet recharge");
//
//                paymentTransactionRepository.save(txn);
//
//                System.out.println("✅ WALLET UPDATED SUCCESSFULLY");
//                return;
//            }
//
//            // =========================
//            // 🧾 BOOKING PAYMENT
//            // =========================
//            String bookingId = session.getMetadata().get("bookingId");
//
//            if (bookingId != null) {
//                processBookingPayment(Long.valueOf(bookingId), session.getPaymentIntent());
//            }
//
//            return;
//        }
//
//        // =====================================================
//        // ✅ 2. PAYMENT INTENT SUCCESS (🔥 MOST IMPORTANT FIX)
//        // =====================================================
//        if ("payment_intent.succeeded".equals(event.getType())) {
//
//            try {
//                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
//                        .getObject()
//                        .orElse(null);
//
//                if (intent == null) {
//                    System.out.println("❌ PaymentIntent null");
//                    return;
//                }
//
//                String bookingId = intent.getMetadata().get("bookingId");
//
//                if (bookingId == null) {
//                    System.out.println("❌ bookingId missing in intent metadata");
//                    return;
//                }
//
//                System.out.println("🔥 PAYMENT INTENT SUCCESS FOR BOOKING: " + bookingId);
//
//                processBookingPayment(Long.valueOf(bookingId), intent.getId());
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            return;
//        }
//
//        // =====================================================
//        // ✅ 3. CHARGE EVENT (BACKUP SAFETY)
//        // =====================================================
//        if (event.getType().startsWith("charge.")) {
//
//            try {
//
//                JsonObject eventJson = JsonParser.parseString(event.toJson()).getAsJsonObject();
//
//                JsonObject chargeObject =
//                        eventJson.getAsJsonObject("data").getAsJsonObject("object");
//
//                JsonObject metadata = chargeObject.getAsJsonObject("metadata");
//
//                if (metadata == null || !metadata.has("bookingId")) {
//                    System.out.println("❌ bookingId missing in charge metadata");
//                    return;
//                }
//
//                String bookingId = metadata.get("bookingId").getAsString();
//                String paymentIntent = chargeObject.get("payment_intent").getAsString();
//
//                System.out.println("🔥 CHARGE SUCCESS FOR BOOKING: " + bookingId);
//
//                processBookingPayment(Long.valueOf(bookingId), paymentIntent);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//    public void processBookingPayment(Long bookingId, String paymentIntent) {
//
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new RuntimeException("Booking not found"));
//
//        // ✅ Idempotency (VERY IMPORTANT)
//        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
//            System.out.println("Already paid");
//            return;
//        }
//
//        booking.setStripePaymentIntent(paymentIntent);
//
//        markBookingAsPaid(booking);
//        PaymentTransaction txn = new PaymentTransaction();
//        txn.setUserId(booking.getUser().getId());
//        txn.setBookingId(booking.getId());
//        txn.setAmount(booking.getFinalPayableAmount());
//        txn.setMethod(PaymentMethod.STRIPE);
//        txn.setStatus(PaymentStatus.PAID);
//        txn.setStripePaymentIntent(paymentIntent);
//        txn.setDescription("Booking payment via Stripe");
//
//        paymentTransactionRepository.save(txn);
//        System.out.println("✅ PAYMENT UPDATED FOR BOOKING: " + bookingId);
//    }
//    public String createWalletRechargeSession(String email, double amount) throws Exception {
//
//        Stripe.apiKey = stripeSecretKey;
//
//        if (amount <= 0) {
//            throw new RuntimeException("Invalid amount");
//        }
//
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        Long userId = user.getId();
//
//        SessionCreateParams params =
//                SessionCreateParams.builder()
//                        .setMode(SessionCreateParams.Mode.PAYMENT)
//
//                        .setSuccessUrl("http://localhost:5173/user?wallet=success")
//                        .setCancelUrl("http://localhost:5173/user?wallet=cancel")
//
//                        .addLineItem(
//                                SessionCreateParams.LineItem.builder()
//                                        .setQuantity(1L)
//                                        .setPriceData(
//                                                SessionCreateParams.LineItem.PriceData.builder()
//                                                        .setCurrency("inr")
//                                                        .setUnitAmount((long) (amount * 100))
//                                                        .setProductData(
//                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
//                                                                        .setName("Wallet Recharge")
//                                                                        .build()
//                                                        )
//                                                        .build()
//                                        )
//                                        .build()
//                        )
//
//                        // ✅ metadata
//                        .putMetadata("userId", userId.toString())
//                        .putMetadata("type", "WALLET_RECHARGE")
//
//                        .setPaymentIntentData(
//                                SessionCreateParams.PaymentIntentData.builder()
//                                        .putMetadata("userId", userId.toString())
//                                        .putMetadata("type", "WALLET_RECHARGE")
//                                        .build()
//                        )
//
//                        .build();
//
//        Session session = Session.create(params);
//
//        return session.getUrl();
//    }
//    public void markBookingAsPaid(Booking booking) {
//
//        booking.setPaymentStatus(PAID);
//        booking.setStatus(CONFIRMED);
//
//        bookingRepository.save(booking);
//
//        System.out.println("✅ Booking marked PAID: " + booking.getId());
//    }
//}
//



package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.PaymentTransactionRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.model.PaymentIntent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.homemakers.homemakers.model.BookingStatus.CONFIRMED;
import static com.homemakers.homemakers.model.PaymentStatus.PAID;

@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final UserWalletService userWalletService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    public PaymentService(BookingRepository bookingRepository,
                          UserWalletService userWalletService) {
        this.bookingRepository = bookingRepository;
        this.userWalletService = userWalletService;
    }
    public String createCheckoutSession(Long bookingId) throws Exception {

        Stripe.apiKey = stripeSecretKey;

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // ✅ Booking must be confirmed
        if (booking.getStatus() != CONFIRMED) {
            throw new RuntimeException("Booking must be confirmed before payment");
        }

        // ✅ Payment must be required
        if (booking.getPaymentStatus() != PaymentStatus.PAYMENT_REQUIRED) {
            throw new RuntimeException("Payment is not required for this booking");
        }

        // ✅ Safety check
        if (booking.getFinalPayableAmount() == null) {
            throw new RuntimeException("Invalid booking payment state");
        }

        double amountToPay = booking.getFinalPayableAmount();

        // ✅ Handle full wallet payment case
        if (amountToPay <= 0) {
            throw new RuntimeException("No payment required. Fully paid via wallet.");
        }

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)

                        .setSuccessUrl("http://localhost:5173/user/payments?status=success")
                        .setCancelUrl("http://localhost:5173/user/payments?status=cancel")

                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("inr")
                                                        // ✅ FIXED: use remaining amount only
                                                        .setUnitAmount((long) (amountToPay * 100))
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Homemakers Monthly Service")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )

                        // ✅ metadata on session
                        .putMetadata("bookingId", booking.getId().toString())

                        // ✅ metadata on payment intent
                        .setPaymentIntentData(
                                SessionCreateParams.PaymentIntentData.builder()
                                        .putMetadata("bookingId", booking.getId().toString())
                                        .build()
                        )

                        .build();

        Session session = Session.create(params);

        booking.setStripeSessionId(session.getId());
        bookingRepository.save(booking);

        return session.getUrl();
    }
    // =========================================================
    // 🔥 STRIPE WEBHOOK HANDLER (FINAL FIXED)
    // =========================================================
    public void handleStripeEvent(Event event) {
        System.out.println("🚀 HANDLE STRIPE EVENT CALLED");
        Stripe.apiKey = stripeSecretKey;

        System.out.println("🔥 EVENT TYPE: " + event.getType());

        // =====================================================
        // ✅ 1. CHECKOUT SESSION COMPLETED
        // =====================================================
        if ("checkout.session.completed".equals(event.getType())) {

            var optionalObj = event.getDataObjectDeserializer().getObject();

            if (optionalObj.isEmpty()) {
                System.out.println("❌ Session deserialization failed");
                return;
            }

            Session session = (Session) optionalObj.get();

            // 🔥 DEBUG
            System.out.println("SESSION JSON: " + session.toJson());

            String type = null;
            String userId = null;

            // 1️⃣ Try session metadata
            if (session.getMetadata() != null) {
                type = session.getMetadata().get("type");
                userId = session.getMetadata().get("userId");
            }

            // 2️⃣ 🔥 FALLBACK → PaymentIntent metadata
            if (type == null || userId == null) {
                try {
                    PaymentIntent intent = PaymentIntent.retrieve(session.getPaymentIntent());

                    if (intent.getMetadata() != null) {
                        type = intent.getMetadata().get("type");
                        userId = intent.getMetadata().get("userId");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Error fetching PaymentIntent metadata");
                    e.printStackTrace();
                }
            }

            System.out.println("TYPE: " + type);
            System.out.println("USER ID: " + userId);

            // =====================================================
            // 💰 WALLET RECHARGE FLOW
            // =====================================================
            if ("WALLET_RECHARGE".equals(type) && userId != null) {

                double amount = session.getAmountTotal() / 100.0;

                String paymentIntentId = session.getPaymentIntent();

                processWalletRechargeSafe(
                        Long.valueOf(userId),
                        amount,
                        paymentIntentId
                );

                PaymentTransaction txn = new PaymentTransaction();
                txn.setUserId(Long.valueOf(userId));
                txn.setAmount(amount);
                txn.setMethod(PaymentMethod.STRIPE);
                txn.setStatus(PaymentStatus.PAID);
                txn.setDescription("Wallet recharge");

                paymentTransactionRepository.save(txn);

                System.out.println("✅ WALLET UPDATED SUCCESSFULLY");
                return;
            }

            // =====================================================
            // 🧾 BOOKING PAYMENT FLOW
            // =====================================================
            String bookingId = session.getMetadata() != null
                    ? session.getMetadata().get("bookingId")
                    : null;

            if (bookingId != null) {
                processBookingPayment(Long.valueOf(bookingId), session.getPaymentIntent());
            }

            return;
        }

        // =====================================================
        // ✅ 2. PAYMENT INTENT SUCCESS (BACKUP)
        // =====================================================
        if ("payment_intent.succeeded".equals(event.getType())) {

            try {
                PaymentIntent intent = (PaymentIntent) event
                        .getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

                if (intent == null) {
                    System.out.println("❌ PaymentIntent null");
                    return;
                }

                String type = intent.getMetadata().get("type");

                // 🔥 WALLET FALLBACK HERE ALSO
                if ("WALLET_RECHARGE".equals(type)) {

                    String userId = intent.getMetadata().get("userId");

                    double amount = intent.getAmount() / 100.0;

                    processWalletRechargeSafe(
                            Long.valueOf(userId),
                            amount,
                            intent.getId()
                    );

                    System.out.println("✅ WALLET UPDATED (payment_intent)");
                    return;
                }

                String bookingId = intent.getMetadata().get("bookingId");

                if (bookingId != null) {
                    processBookingPayment(Long.valueOf(bookingId), intent.getId());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        // =====================================================
        // ✅ 3. CHARGE EVENT (LAST SAFETY)
        // =====================================================
        if (event.getType().startsWith("charge.")) {

            try {

                JsonObject eventJson = JsonParser.parseString(event.toJson()).getAsJsonObject();

                JsonObject chargeObject =
                        eventJson.getAsJsonObject("data").getAsJsonObject("object");

                JsonObject metadata = chargeObject.getAsJsonObject("metadata");

                if (metadata != null && metadata.has("userId")
                        && "WALLET_RECHARGE".equals(metadata.get("type").getAsString())) {

                    Long userId = Long.valueOf(metadata.get("userId").getAsString());
                    double amount = chargeObject.get("amount").getAsDouble() / 100.0;

                    String paymentIntentId = chargeObject
                            .get("payment_intent")
                            .getAsString();

                    processWalletRechargeSafe(
                            userId,
                            amount,
                            paymentIntentId
                    );

                    System.out.println("✅ WALLET UPDATED (charge fallback)");
                    return;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // =========================================================
    // 🧾 BOOKING PAYMENT PROCESSOR
    // =========================================================
    public void processBookingPayment(Long bookingId, String paymentIntent) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            System.out.println("Already paid");
            return;
        }

        booking.setStripePaymentIntent(paymentIntent);

        markBookingAsPaid(booking);

        PaymentTransaction txn = new PaymentTransaction();
        txn.setUserId(booking.getUser().getId());
        txn.setBookingId(booking.getId());
        txn.setAmount(booking.getFinalPayableAmount());
        txn.setMethod(PaymentMethod.STRIPE);
        txn.setStatus(PaymentStatus.PAID);
        txn.setStripePaymentIntent(paymentIntent);
        txn.setDescription("Booking payment via Stripe");

        paymentTransactionRepository.save(txn);

        System.out.println("✅ PAYMENT UPDATED FOR BOOKING: " + bookingId);
    }

    // =========================================================
    // 💰 CREATE WALLET RECHARGE SESSION
    // =========================================================
    public String createWalletRechargeSession(String email, double amount) throws Exception {

        Stripe.apiKey = stripeSecretKey;

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long userId = user.getId();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:5173/user?wallet=success")
                        .setCancelUrl("http://localhost:5173/user?wallet=cancel")

                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("inr")
                                                        .setUnitAmount((long) (amount * 100))
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Wallet Recharge")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )

                        .putMetadata("userId", userId.toString())
                        .putMetadata("type", "WALLET_RECHARGE")

                        .setPaymentIntentData(
                                SessionCreateParams.PaymentIntentData.builder()
                                        .putMetadata("userId", userId.toString())
                                        .putMetadata("type", "WALLET_RECHARGE")
                                        .build()
                        )

                        .build();

        Session session = Session.create(params);

        return session.getUrl();
    }

    // =========================================================
    // ✅ MARK BOOKING PAID
    // =========================================================
    public void markBookingAsPaid(Booking booking) {

        booking.setPaymentStatus(PAID);
        booking.setStatus(CONFIRMED);

        bookingRepository.save(booking);

        System.out.println("✅ Booking marked PAID: " + booking.getId());
    }
    private void processWalletRechargeSafe(Long userId, double amount, String paymentIntentId) {

        if (paymentIntentId == null) {
            System.out.println("❌ PaymentIntent NULL — skipping");
            return;
        }

        boolean exists = paymentTransactionRepository
                .existsByStripePaymentIntent(paymentIntentId);

        if (exists) {
            System.out.println("⚠️ DUPLICATE PAYMENT BLOCKED: " + paymentIntentId);
            return;
        }

        userWalletService.addMoney(userId, amount);

        PaymentTransaction txn = new PaymentTransaction();
        txn.setUserId(userId);
        txn.setAmount(amount);
        txn.setMethod(PaymentMethod.STRIPE);
        txn.setStatus(PaymentStatus.PAID);
        txn.setStripePaymentIntent(paymentIntentId);
        txn.setDescription("Wallet recharge");

        paymentTransactionRepository.save(txn);

        System.out.println("✅ WALLET UPDATED ONCE ONLY");
    }
}