package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // ✅ Checks if a provider has an active booking that occupies the given date+time.
    //
    // Monthly service logic:
    // - A booking occupies the provider EVERY DAY for ~30 days at the same daily hours.
    // - So if provider is booked 12:00–1:00 PM from May 1, they are occupied
    //   12:00–1:00 PM on May 2, May 10, May 25, etc.
    //
    // Two cases:
    // 1. Work has started (workStartDate set):
    //    Block workStartDate → workEndDate (or workStartDate+30 if not ended yet)
    //
    // 2. Confirmed but not started (workStartDate IS NULL):
    //    Use availability.date as range start → availability.date + 30 days
    //    *** estimatedEnd is passed as availability.date + 30 from the service layer ***
    //
    // The key fix: estimatedEnd must be computed from booking's availability.date,
    // NOT from the new slot's date. Service layer now passes:
    //   existsActiveBookingCoveringDateAndTime(providerId, newDate, newStart, newEnd)
    // and the query computes the range from b.availability.date internally.
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

    // ✅ Used by service layer to get all active bookings for this provider
    // so we can compute estimatedEnd from each booking's availability.date
    @Query("""
        SELECT b FROM Booking b
        WHERE b.provider.id = :providerId
          AND b.status IN (
              com.homemakers.homemakers.model.BookingStatus.CONFIRMED,
              com.homemakers.homemakers.model.BookingStatus.SERVICE_IN_PROGRESS
          )
    """)
    List<Booking> findActiveBookingsForProvider(@Param("providerId") Long providerId);
    Optional<Booking> findByIdAndUser_Email(Long id, String email);
}