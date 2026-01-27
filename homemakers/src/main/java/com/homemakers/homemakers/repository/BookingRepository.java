package com.homemakers.homemakers.repository;


import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByAvailabilityId(Long availabilityId);

    List<Booking> findByUser(User user);

    List<Booking> findByProvider(Provider provider);

    Optional<Booking> findByIdAndProvider_User_Email(Long id, String email);
    List<Booking> findByUser_Email(String email);

    List<Booking> findByProvider_User_Email(String email);
    boolean existsByAvailability(ProviderAvailability availability);

    List<Booking> findByProviderAndStatus(
            Provider provider,
            BookingStatus status
    );
    @Query("""
        SELECT b FROM Booking b
        WHERE b.provider = :provider
        AND b.status = com.homemakers.homemakers.model.BookingStatus.COMPLETED
        AND FUNCTION('YEAR_MONTH', b.completedAt) =
            FUNCTION('YEAR_MONTH', CURRENT_DATE)
    """)
    List<Booking> findCompletedMonthlyBookings(
            @Param("provider") Provider provider
    );
}
