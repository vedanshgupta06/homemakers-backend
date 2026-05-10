package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import com.homemakers.homemakers.model.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByAvailabilityId(Long availabilityId);

    List<Booking> findByUser(User user);

    List<Booking> findByProvider(Provider provider);

    Optional<Booking> findByIdAndProvider_User_Email(Long id, String email);

    List<Booking> findByUser_Email(String email);

    List<Booking> findByProvider_User_Email(String email);

    boolean existsByAvailability(ProviderAvailability availability);

    List<Booking> findByProviderAndStatus(Provider provider, BookingStatus status);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.provider = :provider
        AND b.status = com.homemakers.homemakers.model.BookingStatus.COMPLETED
        AND FUNCTION('YEAR_MONTH', b.completedAt) =
            FUNCTION('YEAR_MONTH', CURRENT_DATE)
    """)
    List<Booking> findCompletedMonthlyBookings(@Param("provider") Provider provider);

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

    List<Booking> findByUser_EmailAndPaymentStatus(String email, PaymentStatus paymentStatus);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findByStatusAndPaymentStatus(BookingStatus status, PaymentStatus paymentStatus);

    int countByUserEmail(String email);

    int countByUserEmailAndStatus(String email, BookingStatus status);

    List<Booking> findTop5ByUserOrderByCreatedAtDesc(User user);

    long countByUserAndStatusInAndUpdatedAtAfter(
            User user, List<BookingStatus> statuses, LocalDateTime after
    );

    Optional<Booking> findByAvailability_Id(Long availabilityId);

    Optional<Booking> findByIdAndUser_Email(Long id, String email);

    boolean existsByAvailabilityAndStatusIn(
            ProviderAvailability availability,
            List<BookingStatus> statuses
    );

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.provider.id = :providerId
          AND b.status IN (
              com.homemakers.homemakers.model.BookingStatus.CONFIRMED,
              com.homemakers.homemakers.model.BookingStatus.SERVICE_IN_PROGRESS
          )
          AND b.bookingStartTime < :endTime
          AND b.bookingEndTime > :startTime
          AND (
              (b.workStartDate IS NOT NULL
                AND b.workStartDate <= :date
                AND (b.workEndDate IS NULL OR b.workEndDate >= :date))
              OR
              (b.workStartDate IS NULL
                AND b.availability.date <= :date
                AND :date <= :bookingEstimatedEnd)
          )
    """)
    boolean existsActiveBookingCoveringDateAndTime(
            @Param("providerId") Long providerId,
            @Param("date") LocalDate date,
            @Param("bookingEstimatedEnd") LocalDate bookingEstimatedEnd,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.provider.id = :providerId
          AND b.status IN (
              com.homemakers.homemakers.model.BookingStatus.CONFIRMED,
              com.homemakers.homemakers.model.BookingStatus.SERVICE_IN_PROGRESS
          )
    """)
    List<Booking> findActiveBookingsForProvider(@Param("providerId") Long providerId);

    // =========================================================
    // ✅ FETCH JOIN queries — eliminates N+1 for scheduler jobs
    // Each loads booking + all related data in ONE SQL query
    // Use these in BookingExpiryService instead of findByStatus()
    // =========================================================

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.availability
        LEFT JOIN FETCH b.provider p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.services
        WHERE b.status = :status
    """)
    List<Booking> findByStatusWithDetails(@Param("status") BookingStatus status);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.availability
        LEFT JOIN FETCH b.provider p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.services
        WHERE b.status = :status AND b.createdAt < :before
    """)
    List<Booking> findByStatusAndCreatedAtBeforeWithDetails(
            @Param("status") BookingStatus status,
            @Param("before") LocalDateTime before
    );

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.availability
        LEFT JOIN FETCH b.provider p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.services
        WHERE b.status = :status AND b.paymentStatus = :paymentStatus
    """)
    List<Booking> findByStatusAndPaymentStatusWithDetails(
            @Param("status") BookingStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );

    // =========================================================
    // ✅ Nullify availability FK before deleting a slot
    // Used in ProviderAvailabilityController.deleteSlot()
    // =========================================================
    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.availability = null WHERE b.availability.id = :slotId")
    void nullifyAvailability(@Param("slotId") Long slotId);
    List<Booking> findByStatusAndPaymentStatusAndConfirmedAtBefore(
            BookingStatus status,
            PaymentStatus paymentStatus,
            LocalDateTime confirmedAt
    );
}