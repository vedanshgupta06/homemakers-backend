package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderPayoutTransaction;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.model.WeeklyPayoutStatus;
import com.homemakers.homemakers.repository.ProviderPayoutTransactionRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderPayoutHistoryService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final ProviderPayoutTransactionRepository txnRepo;

    public ProviderPayoutHistoryService(
            ProviderRepository providerRepository,
            UserRepository userRepository,
            ProviderPayoutTransactionRepository txnRepo
    ) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.txnRepo = txnRepo;
    }

    public List<ProviderPayoutTransaction> getPaidWeeklyPayouts(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        return txnRepo.findByProviderAndStatusOrderByPaidAtDesc(
                provider,
                WeeklyPayoutStatus.PAID
        );
    }
}
