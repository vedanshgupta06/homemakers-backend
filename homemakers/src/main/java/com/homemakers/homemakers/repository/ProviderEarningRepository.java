package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
            @Param("status") EarningStatus status
    );


    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM ProviderEarning e
        WHERE e.provider = :provider
          AND e.booking = :booking
          AND e.status = 'AVAILABLE'
    """)
    double sumByProviderAndBooking(
            @Param("provider") Provider provider,
            @Param("booking") Booking booking
    );
    boolean existsByProviderAndBookingAndWeekNo(
            Provider provider,
            Booking booking,
            int weekNo
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT e FROM ProviderEarning e
    WHERE e.provider = :provider
    AND e.status = :status
""")
    List<ProviderEarning> findAllForPayoutLock(
            @Param("provider") Provider provider,
            @Param("status") EarningStatus status
    );
    List<ProviderEarning> findByStatus(EarningStatus status);
    List<ProviderEarning> findByPayout(ProviderPayout payout);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT e FROM ProviderEarning e
    WHERE e.provider = :provider
    AND e.status = com.homemakers.homemakers.model.EarningStatus.AVAILABLE
""")
    List<ProviderEarning> findAvailableForUpdate(@Param("provider") Provider provider);

}
