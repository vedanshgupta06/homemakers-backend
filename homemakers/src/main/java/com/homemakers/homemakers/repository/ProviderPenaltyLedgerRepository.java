package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderPenaltyLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderPenaltyLedgerRepository
        extends JpaRepository<ProviderPenaltyLedger, Long> {

    // Used during settlement to deduct pending penalties
    List<ProviderPenaltyLedger> findByProviderAndDeductedFalse(Provider provider);

    // Used for admin view
    List<ProviderPenaltyLedger> findByProvider(Provider provider);
    boolean existsByBooking(Booking booking); // ✅ prevents duplicate penalty
}