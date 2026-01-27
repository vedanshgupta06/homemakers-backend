package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderPayoutTransactionRepository
        extends JpaRepository<ProviderPayoutTransaction, Long> {


    List<ProviderPayoutTransaction>
    findByStatus(WeeklyPayoutStatus status);
    List<ProviderPayoutTransaction>
    findByProviderAndBookingAndPayoutMonth(
            Provider provider,
            Booking booking,
            String payoutMonth
    );

    Optional<ProviderPayoutTransaction> findByProviderAndBookingAndPayoutMonthAndWeekNo(
            Provider provider,
            Booking booking,
            String payoutMonth,
            int weekNo
    );

    boolean existsByProviderAndBookingAndPayoutMonthAndWeekNo(
            Provider provider,
            Booking booking,
            String payoutMonth,
            int weekNo
    );
    List<ProviderPayoutTransaction>
    findByProviderAndStatusOrderByPaidAtDesc(
            Provider provider,
            WeeklyPayoutStatus status
    );

}

