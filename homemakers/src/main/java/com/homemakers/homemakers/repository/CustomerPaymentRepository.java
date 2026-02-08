package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPaymentRepository
        extends JpaRepository<CustomerPayment, Long> {
}
