package com.homemakers.homemakers.controller;



import com.homemakers.homemakers.model.UserWalletTransaction;
import com.homemakers.homemakers.service.UserWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/user/wallet")
public class UserWalletController {

    @Autowired
    private UserWalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<Double> getBalance(Authentication authentication) {

        String email = authentication.getName(); // from JWT

        return ResponseEntity.ok(walletService.getBalanceByEmail(email));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<UserWalletTransaction>> getTransactions(Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(walletService.getTransactionsByEmail(email));
    }
}
