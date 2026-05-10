package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.PaymentTransaction;
import com.homemakers.homemakers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);


    List<PaymentTransaction> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByStripePaymentIntent(String stripePaymentIntent);
    Optional<PaymentTransaction> findByBookingId(Long bookingId);

    List<PaymentTransaction> findAllByBookingId(Long bookingId);
}