package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.DeductionState;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderDeduction;
import com.homemakers.homemakers.model.ProviderPayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderDeductionRepository
        extends JpaRepository<ProviderDeduction, Long> {

    List<ProviderDeduction> findByPayout(ProviderPayout payout);
    List<ProviderDeduction> findByProviderAndStateIn(
            Provider provider,
            List<DeductionState> states
    );
    List<ProviderDeduction> findByProviderOrderByCreatedAtDesc(Provider provider);

}

