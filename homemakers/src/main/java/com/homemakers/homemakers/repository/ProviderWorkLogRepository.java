package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProviderWorkLogRepository
        extends JpaRepository<ProviderWorkLog, Long> {

    List<ProviderWorkLog> findByProviderAndBooking(
            Provider provider,
            Booking booking
    );

    long countByProviderAndBookingAndStatus(
            Provider provider,
            Booking booking,
            WorkStatus status
    );

    List<ProviderWorkLog> findByProviderAndBookingAndStatus(
            Provider provider,
            Booking booking,
            WorkStatus status
    );

}
