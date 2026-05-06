package com.homemakers.homemakers.repository;



import com.homemakers.homemakers.model.UserWalletTransaction;
import com.homemakers.homemakers.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserWalletTransactionRepository extends JpaRepository<UserWalletTransaction, Long> {

    List<UserWalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByBookingIdAndType(Long bookingId, TransactionType type);
    List<UserWalletTransaction> findTop5ByUserIdAndTypeOrderByCreatedAtDesc(
            Long userId, TransactionType type
    );
}