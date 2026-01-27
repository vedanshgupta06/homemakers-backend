package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface ProviderEarningRepository
        extends JpaRepository<ProviderEarning, Long> {

    List<ProviderEarning> findByProvider(Provider provider);

    List<ProviderEarning> findByProviderAndStatus(
            Provider provider,
            EarningStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM ProviderEarning e
        WHERE e.provider = :provider
          AND e.status = :status
    """)
    Double sumByProviderAndStatus(
            Provider provider,
            EarningStatus status
    );
    @Query("""
    SELECT COALESCE(SUM(e.amount), 0)
    FROM ProviderEarning e
    WHERE e.provider = :provider
      AND e.status = :status
""")
double sumAmountByProviderAndStatus(
        @Param("provider") Provider provider,
        @Param("status") PayoutStatus status
);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM ProviderEarning e
        WHERE e.provider = :provider
          AND e.booking = :booking
          AND e.status = 'PENDING'
    """)
    double sumByProviderAndBooking(
            @Param("provider") Provider provider,
            @Param("booking") Booking booking
    );
}
