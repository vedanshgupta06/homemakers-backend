//package com.homemakers.homemakers.repository;
//
//import com.homemakers.homemakers.model.Provider;
//import com.homemakers.homemakers.model.ProviderAvailability;
//import jakarta.persistence.LockModeType;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Lock;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Optional;
//
//public interface ProviderAvailabilityRepository
//        extends JpaRepository<ProviderAvailability, Long> {
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    Optional<ProviderAvailability> findWithLockById(Long id);
//    List<ProviderAvailability> findByProviderAndDateAndActiveTrue(
//            Provider provider,
//            LocalDate date
//    );
//
//    boolean existsByProviderAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
//            Provider provider,
//            LocalDate date,
//            LocalTime endTime,
//            LocalTime startTime
//    );
//
//
//    List<ProviderAvailability> findByDateAndActiveTrue(LocalDate date);
//
//    List<ProviderAvailability> findByProvider_Id(Long providerId);
//
//    boolean existsByProviderAndActiveTrue(Provider provider);
//
//    List<ProviderAvailability> findByProvider(Provider provider);
//
//}

package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderAvailability;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ProviderAvailabilityRepository
        extends JpaRepository<ProviderAvailability, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProviderAvailability> findWithLockById(Long id);

    List<ProviderAvailability> findByProviderAndDateAndActiveTrue(
            Provider provider,
            LocalDate date
    );

    boolean existsByProviderAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Provider provider,
            LocalDate date,
            LocalTime endTime,
            LocalTime startTime
    );

    List<ProviderAvailability> findByDateAndActiveTrue(LocalDate date);

    List<ProviderAvailability> findByProvider_Id(Long providerId);

    boolean existsByProviderAndActiveTrue(Provider provider);

    List<ProviderAvailability> findByProvider(Provider provider);

    // ── Used in rejectBooking() to find and clean up the after-slot ──
    Optional<ProviderAvailability> findByProviderAndDateAndStartTime(
            Provider provider,
            LocalDate date,
            LocalTime startTime
    );
}