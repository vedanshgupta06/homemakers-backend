package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProviderLeaveLedgerRepository
        extends JpaRepository<ProviderLeaveLedger, Long> {

    List<ProviderLeaveLedger> findByProviderAndBooking(
            Provider provider,
            Booking booking
    );

    long countByProviderAndBookingAndLeaveType(
            Provider provider,
            Booking booking,
            LeaveType leaveType
    );
    boolean existsByProviderAndBookingAndLeaveDate(
            Provider provider,
            Booking booking,
            LocalDate leaveDate
    );

}
