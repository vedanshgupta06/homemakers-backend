package com.homemakers.homemakers.service;

import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class UserWalletService {

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private UserWalletTransactionRepository txnRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ NEW HELPER METHOD (CORE FIX)
    private UserWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> createWallet(userId));
    }

    private UserWallet createWallet(Long userId) {
        UserWallet wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        wallet.setReservedBalance(0.0); // ✅ IMPORTANT
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    // =========================================================
    // ADD MONEY / REFUND
    // =========================================================

    @Transactional
    public void addRefund(Long userId, Long bookingId, Double amount, String description) {

        UserWallet wallet = getOrCreateWallet(userId); // ✅ FIXED

        double newBalance = wallet.getBalance() + amount;

        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        UserWalletTransaction txn = new UserWalletTransaction();
        txn.setUserId(userId);
        txn.setBookingId(bookingId);
        txn.setAmount(amount);
        txn.setType(TransactionType.REFUND);
        txn.setBalanceAfterTransaction(newBalance);
        txn.setDescription(description);
        txn.setCreatedAt(LocalDateTime.now());

        txnRepository.save(txn);
    }

    @Transactional
    public void addMoney(Long userId, double amount) {

        UserWallet wallet = getOrCreateWallet(userId);

        System.out.println("💰 BEFORE: " + wallet.getBalance());

        double newBalance = wallet.getBalance() + amount;

        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.saveAndFlush(wallet); // 🔥🔥🔥 MUST

        System.out.println("💰 AFTER: " + wallet.getBalance());
    }

    // =========================================================
    // RESERVE FLOW (FIXED 🔥)
    // =========================================================

    @Transactional
    public double reserveAmount(Long userId, Long bookingId, double amount) {

        UserWallet wallet = getOrCreateWallet(userId); // ✅ FIXED

        double availableBalance = wallet.getBalance() - wallet.getReservedBalance();

        double reserved = Math.min(availableBalance, amount);

        if (reserved <= 0) return 0;

        wallet.setReservedBalance(wallet.getReservedBalance() + reserved);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        UserWalletTransaction txn = new UserWalletTransaction();
        txn.setUserId(userId);
        txn.setBookingId(bookingId);
        txn.setAmount(reserved);
        txn.setType(TransactionType.RESERVE);
        txn.setBalanceAfterTransaction(wallet.getBalance());
        txn.setDescription("Wallet amount reserved");
        txn.setCreatedAt(LocalDateTime.now());

        txnRepository.save(txn);

        return reserved;
    }

    @Transactional
    public void confirmReservedAmount(Long userId, Long bookingId, double amount) {

        UserWallet wallet = getOrCreateWallet(userId); // ✅ FIXED

        wallet.setReservedBalance(wallet.getReservedBalance() - amount);
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);

        UserWalletTransaction txn = new UserWalletTransaction();
        txn.setUserId(userId);
        txn.setBookingId(bookingId);
        txn.setAmount(-amount);
        txn.setType(TransactionType.DEBIT);
        txn.setBalanceAfterTransaction(wallet.getBalance());
        txn.setDescription("Wallet deducted after provider acceptance");
        txn.setCreatedAt(LocalDateTime.now());

        txnRepository.save(txn);
    }

    @Transactional
    public void releaseReservedAmount(Long userId, Long bookingId, double amount) {

        boolean alreadyReleased = txnRepository
                .existsByBookingIdAndType(bookingId, TransactionType.RELEASE);

        if (alreadyReleased) return;

        UserWallet wallet = getOrCreateWallet(userId); // ✅ FIXED

        if (wallet.getReservedBalance() < amount) {
            throw new RuntimeException("Invalid release amount");
        }

        wallet.setReservedBalance(wallet.getReservedBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);

        UserWalletTransaction txn = new UserWalletTransaction();
        txn.setUserId(userId);
        txn.setBookingId(bookingId);
        txn.setAmount(amount);
        txn.setType(TransactionType.RELEASE);
        txn.setBalanceAfterTransaction(wallet.getBalance());
        txn.setDescription("Reservation expired - amount released");
        txn.setCreatedAt(LocalDateTime.now());

        txnRepository.save(txn);
    }

    // =========================================================
    // READ OPERATIONS
    // =========================================================

    public Double getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(UserWallet::getBalance)
                .orElse(0.0);
    }

    public List<UserWalletTransaction> getTransactions(Long userId) {
        return txnRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public double getBalanceByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getBalance(user.getId());
    }

    public List<UserWalletTransaction> getTransactionsByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getTransactions(user.getId());
    }
    @Transactional
    public void refundToWallet(Long userId, double amount, String description) {

        UserWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        double newBalance = wallet.getBalance() + amount;  // ✅ calculate first

        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        UserWalletTransaction txn = new UserWalletTransaction();
        txn.setUserId(userId);
        txn.setAmount(amount);
        txn.setType(TransactionType.REFUND);
        txn.setBalanceAfterTransaction(newBalance);  // ✅ this was missing
        txn.setDescription(description);
        txn.setCreatedAt(LocalDateTime.now());
        txnRepository.save(txn);
    }
}