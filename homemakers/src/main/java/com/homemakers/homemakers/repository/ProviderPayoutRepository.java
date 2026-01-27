package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderPayoutRepository
        extends JpaRepository<ProviderPayout, Long> {

    List<ProviderPayout> findByProvider(Provider provider);

    List<ProviderPayout> findByProviderAndStatus(
            Provider provider,
            PayoutStatus status
    );

    @Query("""
    SELECT COALESCE(SUM(p.netPayout), 0)
    FROM ProviderPayout p
    WHERE p.provider = :provider
      AND p.status = :status
""")
    double sumByProviderAndStatus(
            @Param("provider") Provider provider,
            @Param("status") PayoutStatus status
    );


    Optional<ProviderPayout> findByProviderAndPayoutMonth(
            Provider provider,
            String payoutMonth
    );
    List<ProviderPayout> findByProviderOrderByPayoutMonthDesc(Provider provider);

}


