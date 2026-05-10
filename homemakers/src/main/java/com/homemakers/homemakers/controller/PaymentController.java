package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.PaymentTransaction;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.PaymentTransactionRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    public PaymentController(PaymentService paymentService,
                             UserRepository userRepository,
                             PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @PostMapping("/booking/{bookingId}")
    @PreAuthorize("hasRole('USER')")
    public Map<String, String> createPaymentSession(@PathVariable Long bookingId) throws Exception {

        String url = paymentService.createCheckoutSession(bookingId);

        return Map.of("url", url);
    }
    @PostMapping("/wallet/recharge/{amount}")
    @PreAuthorize("hasRole('USER')")
    public Map<String, String> rechargeWallet(
            @PathVariable double amount,
            Authentication authentication
    ) throws Exception {

        String email = authentication.getName();

        String url = paymentService.createWalletRechargeSession(email, amount);

        return Map.of("url", url);
    }
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    public List<PaymentTransaction> getHistory(Authentication auth) {

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        return paymentTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}