package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.List;
import com.homemakers.homemakers.model.BookingStatus;

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

    @Query("""
    SELECT b FROM Booking b
    WHERE b.status IN :statuses
    AND b.workStartDate <= :date
    AND (b.workEndDate IS NULL OR b.workEndDate >= :date)
    """)
    List<Booking> findActiveForAttendance(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("date") LocalDate date
    );

    List<Booking> findByStatus(BookingStatus status);
    @Query("""
        SELECT b FROM Booking b
        WHERE b.availability.id = :availabilityId
        AND b.status IN (
            com.homemakers.homemakers.model.BookingStatus.PENDING,
            com.homemakers.homemakers.model.BookingStatus.CONFIRMED,
            com.homemakers.homemakers.model.BookingStatus.SERVICE_IN_PROGRESS
        )
        """)
    List<Booking> findActiveBookingsByAvailability(Long availabilityId);

    Optional<Booking> findByStripeSessionId(String sessionId);
    List<Booking> findByUser_EmailAndPaymentStatus(
            String email,
            PaymentStatus paymentStatus
    );
    List<Booking> findByStatusAndCreatedAtBefore(
            BookingStatus status,
            LocalDateTime time
    );
    List<Booking> findByStatusAndPaymentStatus(
            BookingStatus status,
            PaymentStatus paymentStatus
    );
    // ✅ TOTAL BOOKINGS
    int countByUserEmail(String email);

    // ✅ UPCOMING BOOKINGS
    int countByUserEmailAndStatus(String email, BookingStatus status);
    List<Booking> findTop5ByUserOrderByCreatedAtDesc(User user);
}