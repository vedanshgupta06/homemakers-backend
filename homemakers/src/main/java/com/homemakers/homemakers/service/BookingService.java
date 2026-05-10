package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.*;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.util.GeoUtil;
import com.homemakers.homemakers.util.ServiceDurationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ServicePricingRepository pricingRepository;
    private final ProviderLeaveLedgerRepository leaveLedgerRepository;
    private final BookingCalculationService bookingCalculationService;
    private final ProviderSettlementService providerSettlementService;
    private final UserWalletService userWalletService;
    private final PaymentService paymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProviderNotificationService notificationService;
    private final ProviderWorkLogRepository workLogRepository;

    public BookingService(
            BookingRepository bookingRepository,
            ProviderAvailabilityRepository availabilityRepository,
            UserRepository userRepository,
            ProviderRepository providerRepository,
            ServicePricingRepository pricingRepository,
            ProviderLeaveLedgerRepository leaveLedgerRepository,
            BookingCalculationService bookingCalculationService,
            ProviderSettlementService providerSettlementService,
            UserWalletService userWalletService,
            PaymentService paymentService,
            PaymentTransactionRepository paymentTransactionRepository,
            ProviderNotificationService notificationService,
            ProviderWorkLogRepository workLogRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.pricingRepository = pricingRepository;
        this.leaveLedgerRepository = leaveLedgerRepository;
        this.bookingCalculationService = bookingCalculationService;
        this.providerSettlementService = providerSettlementService;
        this.userWalletService = userWalletService;
        this.paymentService = paymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.notificationService = notificationService;
        this.workLogRepository = workLogRepository;
    }

    @PersistenceContext
    private EntityManager entityManager;

    private static final int BUFFER_MINUTES  = 15;
    private static final int SERVICE_DAYS    = 30;

    // =========================================================
    // CREATE BOOKING
    // =========================================================

    @Transactional
    public Booking createBooking(BookingPreviewRequestDTO request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProviderAvailability slot =
                availabilityRepository.findWithLockById(request.getAvailabilityId())
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.isActive()) throw new RuntimeException("Slot already booked");
        if (bookingRepository.existsByAvailability(slot)) throw new RuntimeException("This slot is already booked");

        Provider provider = slot.getProvider();

        int requiredMinutes = 0;
        double totalMonthlyPrice = 0;
        Set<ServiceType> services = new HashSet<>();

        for (BookingServiceRequestDTO s : request.getServices()) {
            ServiceType type = s.getServiceType();
            services.add(type);

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(provider, type, provider.getCity())
                    .orElseThrow(() -> new RuntimeException("Pricing not set for " + type));

            Integer hours = s.getHours();
            double price = bookingCalculationService.calculatePrice(
                    pricing, type, request.getMembers(), request.getHouseSize(), hours
            );
            totalMonthlyPrice += price;

            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                requiredMinutes += hours * 60;
            } else {
                requiredMinutes += ServiceDurationUtil.getMinutes(type);
            }
        }

        LocalTime requestStart      = slot.getStartTime();
        LocalTime requestEnd        = requestStart.plusMinutes(requiredMinutes);
        LocalTime bufferedEnd       = requestEnd.plusMinutes(BUFFER_MINUTES);
        LocalTime originalSlotStart = slot.getStartTime();
        LocalTime originalSlotEnd   = slot.getEndTime();
        LocalDate slotDate          = slot.getDate();
        LocalDate workEndDate       = slotDate.plusDays(SERVICE_DAYS);

        if (requestEnd.isAfter(slot.getEndTime())) {
            throw new RuntimeException("Requested time outside provider slot");
        }

        availabilityRepository.delete(slot);
        availabilityRepository.flush();

        // 1. Anchor booked slot — inactive, exact booking window, range set
        ProviderAvailability bookedSlot = new ProviderAvailability();
        bookedSlot.setProvider(provider);
        bookedSlot.setDate(slotDate);
        bookedSlot.setStartTime(requestStart);
        bookedSlot.setEndTime(requestEnd);
        bookedSlot.setActive(false);
        bookedSlot.setBookingWorkStart(slotDate);
        bookedSlot.setBookingWorkEnd(workEndDate);
        bookedSlot.setBookingCustomerName(user.getName());
        ProviderAvailability savedBookedSlot = availabilityRepository.save(bookedSlot);

        // 2. Before-slot — free time before booking starts (same day only)
        if (requestStart.isAfter(originalSlotStart)) {
            ProviderAvailability beforeSlot = new ProviderAvailability();
            beforeSlot.setProvider(provider);
            beforeSlot.setDate(slotDate);
            beforeSlot.setStartTime(originalSlotStart);
            beforeSlot.setEndTime(requestStart);
            beforeSlot.setActive(true);
            availabilityRepository.save(beforeSlot);
        }

        // 3. After-slot (split remainder) — free time after booking + buffer (same day only)
        if (bufferedEnd.isBefore(originalSlotEnd)) {
            ProviderAvailability afterSlot = new ProviderAvailability();
            afterSlot.setProvider(provider);
            afterSlot.setDate(slotDate);
            afterSlot.setStartTime(bufferedEnd);
            afterSlot.setEndTime(originalSlotEnd);
            afterSlot.setActive(true);
            availabilityRepository.save(afterSlot);
        }

        // ── Platform fee: 5% of service price, paid by user ──
        double platformFee   = Math.round(totalMonthlyPrice * 0.05 * 100.0) / 100.0;
        double totalWithFee  = Math.round((totalMonthlyPrice + platformFee) * 100.0) / 100.0;

        Booking booking = new Booking();
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUser(user);
        booking.setProvider(provider);
        booking.setAvailability(savedBookedSlot);
        booking.setServices(services);
        booking.setTotalPrice(totalMonthlyPrice);   // base price — what provider earns
        booking.setPlatformFee(platformFee);         // 5% platform cut
        booking.setTotalWithFee(totalWithFee);       // what user actually pays
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingStartTime(requestStart);
        booking.setBookingEndTime(requestEnd);
        booking.setOriginalSlotStart(originalSlotStart);
        booking.setOriginalSlotEnd(originalSlotEnd);

        Booking savedBooking = bookingRepository.save(booking);

        String serviceNames = services.stream()
                .map(s -> s.name().replace("_", " "))
                .reduce((a, b) -> a + ", " + b)
                .orElse("Services");
        notificationService.newBookingRequest(
                provider, savedBooking.getId(), user.getName(), serviceNames
        );

        double availableWallet = userWalletService.getAvailableBalance(user.getId());
        // Wallet eligibility is based on totalWithFee — full amount user owes
        double walletEligible  = Math.min(availableWallet, totalWithFee);

        savedBooking.setWalletUsed(0.0);
        savedBooking.setWalletEligible(walletEligible);
        savedBooking.setFinalPayableAmount(totalWithFee); // user pays total including fee
        savedBooking.setWalletConsentStatus(WalletConsentStatus.PENDING);

        return bookingRepository.save(savedBooking);
    }

    // =========================================================
    // WALLET CONSENT
    // =========================================================

    @Transactional
    public Booking submitWalletConsent(Long bookingId, boolean useWallet, String userEmail) {

        Booking booking = bookingRepository
                .findByIdAndUser_Email(bookingId, userEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Consent can only be given for PENDING bookings");
        }
        if (booking.getWalletConsentStatus() != WalletConsentStatus.PENDING) {
            throw new RuntimeException("Wallet consent already submitted");
        }

        // Use totalWithFee (service + platform fee) as the amount user owes
        double amountOwed = booking.getTotalWithFee() != null
                ? booking.getTotalWithFee()
                : booking.getTotalPrice();

        if (useWallet) {
            double reserved = userWalletService.reserveAmount(
                    booking.getUser().getId(),
                    booking.getId(),
                    amountOwed
            );
            booking.setWalletUsed(reserved);
            booking.setFinalPayableAmount(amountOwed - reserved);
            booking.setWalletConsentStatus(WalletConsentStatus.ACCEPTED);
        } else {
            booking.setWalletUsed(0.0);
            booking.setFinalPayableAmount(amountOwed);
            booking.setWalletConsentStatus(WalletConsentStatus.DECLINED);
        }

        return bookingRepository.save(booking);
    }

    // =========================================================
    // PROVIDER ACCEPT BOOKING
    // =========================================================

    @Transactional
    public Booking acceptBooking(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings allowed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now()); // used for 2-day payment expiry check

        if (booking.getWalletConsentStatus() == WalletConsentStatus.ACCEPTED
                && booking.getWalletUsed() != null
                && booking.getWalletUsed() > 0) {

            userWalletService.confirmReservedAmount(
                    booking.getUser().getId(), booking.getId(), booking.getWalletUsed()
            );

            PaymentTransaction txn = new PaymentTransaction();
            txn.setUserId(booking.getUser().getId());
            txn.setBookingId(booking.getId());
            txn.setAmount(booking.getWalletUsed());
            txn.setMethod(PaymentMethod.WALLET);
            txn.setStatus(PaymentStatus.PAID);
            txn.setDescription("Wallet payment — user consented");
            paymentTransactionRepository.save(txn);
        }

        if (booking.getFinalPayableAmount() != null && booking.getFinalPayableAmount() == 0) {
            notificationService.bookingPaymentReceived(
                    booking.getProvider(),
                    booking.getId(),
                    booking.getWalletUsed() != null ? booking.getWalletUsed() : 0
            );
            paymentService.markBookingAsPaid(booking);
        } else {
            booking.setPaymentStatus(PaymentStatus.PAYMENT_REQUIRED);
        }

        // ✅ Split all future overlapping slots for the 30-day window
        splitAndLockOverlappingFutureSlots(booking);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // PROVIDER START WORK
    // =========================================================

    @Transactional
    public Booking startWork(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Payment not completed");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Booking not confirmed");
        }

        LocalDate today     = LocalDate.now();
        LocalDate startDate = booking.getAvailability().getDate();

        if (today.isBefore(startDate)) throw new RuntimeException("Work cannot be started before the scheduled date");
        if (today.isAfter(startDate))  throw new RuntimeException("Start date has already passed. Contact admin.");

        booking.markWorkStarted(today);
        booking.setStatus(BookingStatus.SERVICE_IN_PROGRESS);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // PROVIDER REJECT BOOKING
    // =========================================================

    @Transactional
    public Booking rejectBooking(Long bookingId, String providerEmail) {

        Booking booking = bookingRepository
                .findByIdAndProvider_User_Email(bookingId, providerEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings allowed");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        notificationService.bookingCancelled(
                booking.getProvider(), booking.getId(), booking.getUser().getName()
        );

        if (booking.getWalletUsed() != null && booking.getWalletUsed() > 0) {
            userWalletService.releaseReservedAmount(
                    booking.getUser().getId(), booking.getId(), booking.getWalletUsed()
            );
        }

        Long providerId         = booking.getProvider().getId();
        Long slotId             = booking.getAvailability().getId();
        LocalDate date          = booking.getAvailability().getDate();
        LocalTime bookingStart  = booking.getBookingStartTime();
        LocalTime bookingEnd    = booking.getBookingEndTime();
        LocalTime originalStart = booking.getOriginalSlotStart();
        LocalTime originalEnd   = booking.getOriginalSlotEnd();
        LocalTime bufferedEnd   = bookingEnd.plusMinutes(BUFFER_MINUTES);

        entityManager.createNativeQuery(
                "UPDATE bookings SET availability_id = NULL WHERE id = :bookingId"
        ).setParameter("bookingId", bookingId).executeUpdate();
        entityManager.flush();

        entityManager.createNativeQuery(
                "DELETE FROM provider_availability WHERE id = :id"
        ).setParameter("id", slotId).executeUpdate();
        entityManager.flush();

        Long afterSlotId = (Long) entityManager.createNativeQuery(
                        "SELECT id FROM provider_availability " +
                                "WHERE provider_id = :pid AND date = :date AND start_time = :start LIMIT 1"
                )
                .setParameter("pid", providerId)
                .setParameter("date", date)
                .setParameter("start", bufferedEnd)
                .getResultStream().findFirst().orElse(null);

        if (afterSlotId != null) {
            Long bookedByBooking = (Long) entityManager.createNativeQuery(
                    "SELECT id FROM bookings WHERE availability_id = :aid LIMIT 1"
            ).setParameter("aid", afterSlotId).getResultStream().findFirst().orElse(null);

            if (bookedByBooking == null) {
                entityManager.createNativeQuery(
                        "DELETE FROM provider_availability WHERE id = :id"
                ).setParameter("id", afterSlotId).executeUpdate();
                saveAvailabilityNative(
                        providerId, date,
                        originalStart != null ? originalStart : bookingStart,
                        originalEnd   != null ? originalEnd   : bookingEnd,
                        true
                );
            } else {
                restoreBeforeSlot(providerId, date, bookingStart, originalStart);
            }
        } else {
            if (originalStart != null && originalStart.isBefore(bookingStart)) {
                restoreBeforeSlot(providerId, date, bookingStart, originalStart);
            } else {
                saveAvailabilityNative(
                        providerId, date,
                        originalStart != null ? originalStart : bookingStart,
                        originalEnd   != null ? originalEnd   : bookingEnd,
                        true
                );
            }
        }

        return bookingRepository.save(booking);
    }

    // =========================================================
    // USER CANCEL BOOKING
    // Only allowed when status is PENDING (before provider accepts).
    // Mirrors rejectBooking slot-restoration logic but is triggered
    // by the user, not the provider.
    // =========================================================

    @Transactional
    public Booking cancelBooking(Long bookingId, String userEmail) {

        Booking booking = bookingRepository
                .findByIdAndUser_Email(bookingId, userEmail)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings can be cancelled by the user");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Refund wallet if payment was already made
        if (booking.getWalletUsed() != null && booking.getWalletUsed() > 0) {
            userWalletService.releaseReservedAmount(
                    booking.getUser().getId(), booking.getId(), booking.getWalletUsed()
            );
        }

        // Restore the original availability slot
        Long providerId         = booking.getProvider().getId();
        Long slotId             = booking.getAvailability() != null ? booking.getAvailability().getId() : null;
        LocalDate date          = booking.getAvailability() != null ? booking.getAvailability().getDate() : null;
        LocalTime bookingStart  = booking.getBookingStartTime();
        LocalTime bookingEnd    = booking.getBookingEndTime();
        LocalTime originalStart = booking.getOriginalSlotStart();
        LocalTime originalEnd   = booking.getOriginalSlotEnd();
        LocalTime bufferedEnd   = bookingEnd != null ? bookingEnd.plusMinutes(BUFFER_MINUTES) : null;

        if (slotId != null && date != null) {
            // Nullify FK reference before deleting the booked slot
            entityManager.createNativeQuery(
                    "UPDATE bookings SET availability_id = NULL WHERE id = :bookingId"
            ).setParameter("bookingId", bookingId).executeUpdate();
            entityManager.flush();

            // Delete the booked (inactive) slot
            entityManager.createNativeQuery(
                    "DELETE FROM provider_availability WHERE id = :id"
            ).setParameter("id", slotId).executeUpdate();
            entityManager.flush();

            // Check if a split remainder (after-slot) exists
            Long afterSlotId = bufferedEnd == null ? null :
                    (Long) entityManager.createNativeQuery(
                                    "SELECT id FROM provider_availability " +
                                            "WHERE provider_id = :pid AND date = :date AND start_time = :start LIMIT 1"
                            )
                            .setParameter("pid", providerId)
                            .setParameter("date", date)
                            .setParameter("start", bufferedEnd)
                            .getResultStream().findFirst().orElse(null);

            if (afterSlotId != null) {
                // Check if the after-slot is itself booked by another booking
                Long bookedByOther = (Long) entityManager.createNativeQuery(
                        "SELECT id FROM bookings WHERE availability_id = :aid LIMIT 1"
                ).setParameter("aid", afterSlotId).getResultStream().findFirst().orElse(null);

                if (bookedByOther == null) {
                    // Safe to delete and restore full original slot
                    entityManager.createNativeQuery(
                            "DELETE FROM provider_availability WHERE id = :id"
                    ).setParameter("id", afterSlotId).executeUpdate();
                    saveAvailabilityNative(
                            providerId, date,
                            originalStart != null ? originalStart : bookingStart,
                            originalEnd   != null ? originalEnd   : bookingEnd,
                            true
                    );
                } else {
                    // After-slot is booked — only restore the before portion
                    restoreBeforeSlot(providerId, date, bookingStart, originalStart);
                }
            } else {
                // No after-slot — restore original slot in full or just before portion
                if (originalStart != null && originalStart.isBefore(bookingStart)) {
                    restoreBeforeSlot(providerId, date, bookingStart, originalStart);
                } else {
                    saveAvailabilityNative(
                            providerId, date,
                            originalStart != null ? originalStart : bookingStart,
                            originalEnd   != null ? originalEnd   : bookingEnd,
                            true
                    );
                }
            }
        }

        notificationService.bookingCancelled(
                booking.getProvider(), booking.getId(), booking.getUser().getName()
        );

        return bookingRepository.save(booking);
    }

    // =========================================================
    // TERMINATE BOOKING
    // =========================================================

    @Transactional
    public Booking terminateBooking(Long bookingId, String reason) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.SERVICE_IN_PROGRESS) {
            throw new RuntimeException("Only active bookings can be terminated");
        }

        booking.setStatus(BookingStatus.TERMINATED);
        booking.setWorkEndDate(LocalDate.now());
        booking.setCompletedAt(LocalDateTime.now());
        booking.setTerminationReason(reason);

        bookingRepository.save(booking);

        if (!booking.isSettlementDone()) {
            providerSettlementService.finalizeSettlement(booking);
            booking.setSettlementDone(true);
            bookingRepository.save(booking);
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {

            long workedDays = workLogRepository.countByProviderAndBookingAndStatusIn(
                    booking.getProvider(), booking,
                    List.of(WorkStatus.CONFIRMED_PRESENT, WorkStatus.PRESENT)
            );

            long leaveDays = workLogRepository.countByProviderAndBookingAndStatus(
                    booking.getProvider(), booking, WorkStatus.LEAVE
            );

            long absentDays = workLogRepository.countByProviderAndBookingAndStatus(
                    booking.getProvider(), booking, WorkStatus.ABSENT
            );

            long paidLeavesAllowed = workedDays > 23 ? 3
                    : workedDays > 15 ? 2
                    : workedDays > 5  ? 1 : 0;

            long nonWorkedDays  = leaveDays + absentDays;
            long paidLeaves     = Math.min(paidLeavesAllowed, nonWorkedDays);
            long chargeableDays = workedDays + paidLeaves;

            double dailyRate    = booking.getTotalPrice() / 30.0;
            double refundAmount = Math.max(0, booking.getTotalPrice() - chargeableDays * dailyRate);

            if (refundAmount > 0) {
                userWalletService.addRefund(
                        booking.getUser().getId(),
                        booking.getId(),
                        refundAmount,
                        "Refund for early termination - Booking #" + booking.getId()
                );
            }
        }

        // ✅ Unlock and restore all future split slots for this booking
        restoreAndUnlockFutureSlots(booking);

        return booking;
    }

    // =========================================================
    // ADMIN COMPLETE BOOKING
    // =========================================================

    @Transactional
    public Booking finalizeBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        providerSettlementService.finalizeSettlement(booking);
        bookingRepository.save(booking);

        // ✅ Unlock and restore all future split slots for this booking
        restoreAndUnlockFutureSlots(booking);

        return booking;
    }

    // =========================================================
    // PRIVATE — Split and lock overlapping future slots on accept
    //
    // When a booking is confirmed, for every provider slot in the
    // next 30 days that overlaps the booking time window:
    //
    //   Original slot: [slotStart -------- slotEnd]
    //   Booking time:        [bookStart -- bookEnd]
    //
    // Result:
    //   Before part:  [slotStart -- bookStart]  active=true  (if exists)
    //   Locked part:  [bookStart -- bookEnd]    active=false, range set
    //   After part:   [bookEnd+buffer -- slotEnd] active=true (if exists)
    //
    // This mirrors exactly what createBooking does for the anchor date,
    // applied to every existing slot in the 30-day window.
    // =========================================================
    private void splitAndLockOverlappingFutureSlots(Booking booking) {
        Provider provider    = booking.getProvider();
        LocalDate anchorDate = booking.getAvailability().getDate();
        LocalDate endDate    = anchorDate.plusDays(SERVICE_DAYS);
        LocalTime bookStart  = booking.getBookingStartTime();
        LocalTime bookEnd    = booking.getBookingEndTime();
        LocalTime bufferedEnd = bookEnd.plusMinutes(BUFFER_MINUTES);
        String customerName  = booking.getUser().getName();
        Long anchorSlotId    = booking.getAvailability().getId(); // ← NEW

        if (bookStart == null || bookEnd == null) return;

        List<ProviderAvailability> allSlots = availabilityRepository.findByProvider(provider);

        for (ProviderAvailability existingSlot : allSlots) {

            // ── Guards ────────────────────────────────────────────────────────────
            if (existingSlot.getId().equals(anchorSlotId)) continue;           // ← NEW: skip anchor slot (booking FK points here)
            if (existingSlot.getDate().isEqual(anchorDate)) continue;          // skip anchor date (handled in createBooking)
            if (existingSlot.getDate().isAfter(endDate)) continue;             // outside 30-day window
            if (!existingSlot.isActive()) continue;                            // already locked/booked
            if (bookingRepository.existsByAvailability(existingSlot)) continue; // ← NEW: FK safety — another booking references this slot
            // ─────────────────────────────────────────────────────────────────────

            LocalTime slotStart = existingSlot.getStartTime();
            LocalTime slotEnd   = existingSlot.getEndTime();

            boolean overlaps = slotStart.isBefore(bookEnd) && slotEnd.isAfter(bookStart);
            if (!overlaps) continue;

            availabilityRepository.delete(existingSlot);
            availabilityRepository.flush();

            LocalDate slotDate = existingSlot.getDate();

            // Part 1 — Before portion
            if (slotStart.isBefore(bookStart)) {
                ProviderAvailability beforePart = new ProviderAvailability();
                beforePart.setProvider(provider);
                beforePart.setDate(slotDate);
                beforePart.setStartTime(slotStart);
                beforePart.setEndTime(bookStart);
                beforePart.setActive(true);
                availabilityRepository.save(beforePart);
            }

            // Part 2 — Locked portion
            LocalTime lockedStart = bookStart.isBefore(slotStart) ? slotStart : bookStart;
            LocalTime lockedEnd   = bookEnd.isAfter(slotEnd)      ? slotEnd   : bookEnd;

            ProviderAvailability lockedPart = new ProviderAvailability();
            lockedPart.setProvider(provider);
            lockedPart.setDate(slotDate);
            lockedPart.setStartTime(lockedStart);
            lockedPart.setEndTime(lockedEnd);
            lockedPart.setActive(false);
            lockedPart.setBookingWorkStart(anchorDate);
            lockedPart.setBookingWorkEnd(endDate);
            lockedPart.setBookingCustomerName(customerName);
            availabilityRepository.save(lockedPart);

            // Part 3 — After portion
            if (bufferedEnd.isBefore(slotEnd)) {
                ProviderAvailability afterPart = new ProviderAvailability();
                afterPart.setProvider(provider);
                afterPart.setDate(slotDate);
                afterPart.setStartTime(bufferedEnd);
                afterPart.setEndTime(slotEnd);
                afterPart.setActive(true);
                availabilityRepository.save(afterPart);
            }

            System.out.println("Split slot on " + slotDate
                    + " [" + slotStart + "-" + slotEnd + "]"
                    + " → locked [" + lockedStart + "-" + lockedEnd + "]"
                    + (slotStart.isBefore(bookStart) ? " + before [" + slotStart + "-" + bookStart + "]" : "")
                    + (bufferedEnd.isBefore(slotEnd) ? " + after [" + bufferedEnd + "-" + slotEnd + "]" : "")
                    + " for booking #" + booking.getId());
        }
    }

    // =========================================================
    // PRIVATE — Restore and unlock all future split slots
    //
    // When booking ends (completed, terminated, or cancelled),
    // find all inactive slots in the 30-day range that belong
    // to this booking and:
    //   1. Clear their booking range fields (bookingWorkStart/End, customerName)
    //   2. Mark them active=true so provider can re-use or re-add
    //
    // The provider must then manually add new slots for those dates.
    // We do NOT auto-merge split parts back — provider decides fresh.
    //
    // Safety: skip slots that have another active booking attached.
    // =========================================================
    private void restoreAndUnlockFutureSlots(Booking booking) {
        if (booking.getAvailability() == null) return;

        Provider provider    = booking.getProvider();
        LocalDate anchorDate = booking.getAvailability().getDate();
        LocalTime bookStart  = booking.getBookingStartTime();
        LocalTime bookEnd    = booking.getBookingEndTime();

        // Use actual work end if available (termination), else full 30-day window
        LocalDate endDate = booking.getWorkEndDate() != null
                ? booking.getWorkEndDate()
                : anchorDate.plusDays(SERVICE_DAYS);

        // 1. Reactivate the anchor slot itself and clear its range
        ProviderAvailability anchorSlot = availabilityRepository
                .findById(booking.getAvailability().getId())
                .orElse(null);

        if (anchorSlot != null && !anchorSlot.isActive()) {
            anchorSlot.setActive(true);
            anchorSlot.setBookingWorkStart(null);
            anchorSlot.setBookingWorkEnd(null);
            anchorSlot.setBookingCustomerName(null);
            availabilityRepository.save(anchorSlot);
            System.out.println("🔓 Reactivated anchor slot #" + anchorSlot.getId());
        }

        if (bookStart == null || bookEnd == null) return;

        // Find all locked slots in the booking's range belonging to this booking.
        // Includes slots BEFORE the anchor (e.g. May 1) since those were also
        // split+locked when the booking was accepted.
        List<ProviderAvailability> allSlots = availabilityRepository.findByProvider(provider);

        for (ProviderAvailability slot : allSlots) {

            // Skip the anchor slot — already handled above
            if (slot.getId().equals(booking.getAvailability().getId())) continue;

            // Only within the booking's full date range (before OR after anchor)
            if (slot.getDate().isAfter(endDate)) continue;

            // Only inactive slots
            if (slot.isActive()) continue;

            // Only slots whose range matches this booking's anchor date
            // This ensures we don't accidentally unlock slots from a different booking
            if (!anchorDate.equals(slot.getBookingWorkStart())) continue;

            // Overlap check: slot time must overlap with booking time window
            boolean overlaps = slot.getStartTime().isBefore(bookEnd)
                    && slot.getEndTime().isAfter(bookStart);
            if (!overlaps) continue;

            // Safety: skip if another booking is actively using this slot
            boolean hasOtherBooking = bookingRepository.existsByAvailability(slot);
            if (hasOtherBooking) continue;

            // Clear booking range and reactivate — provider adds fresh slots manually
            slot.setActive(true);
            slot.setBookingWorkStart(null);
            slot.setBookingWorkEnd(null);
            slot.setBookingCustomerName(null);
            availabilityRepository.save(slot);

            System.out.println("🔓 Unlocked slot #" + slot.getId()
                    + " on " + slot.getDate()
                    + " [" + slot.getStartTime() + "-" + slot.getEndTime() + "]"
                    + " for booking #" + booking.getId());
        }
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private void restoreBeforeSlot(Long providerId, LocalDate date,
                                   LocalTime bookingStart, LocalTime originalStart) {
        if (originalStart != null && originalStart.isBefore(bookingStart)) {
            Long existing = (Long) entityManager.createNativeQuery(
                            "SELECT id FROM provider_availability " +
                                    "WHERE provider_id = :pid AND date = :date AND start_time = :start LIMIT 1"
                    )
                    .setParameter("pid", providerId)
                    .setParameter("date", date)
                    .setParameter("start", originalStart)
                    .getResultStream().findFirst().orElse(null);

            if (existing == null) {
                saveAvailabilityNative(providerId, date, originalStart, bookingStart, true);
            }
        }
    }

    private void saveAvailabilityNative(Long providerId, LocalDate date,
                                        LocalTime start, LocalTime end, boolean active) {
        entityManager.createNativeQuery(
                        "INSERT INTO provider_availability (provider_id, date, start_time, end_time, active) " +
                                "VALUES (:providerId, :date, :start, :end, :active)"
                )
                .setParameter("providerId", providerId)
                .setParameter("date", date)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("active", active)
                .executeUpdate();
    }

    // =========================================================
    // USER BOOKINGS
    // =========================================================

    public List<Booking> getUserBookings(String email) {
        return bookingRepository.findByUser_Email(email);
    }

    // =========================================================
    // PROVIDER BOOKINGS
    // =========================================================

    public List<Booking> getProviderBookings(String providerEmail) {
        Provider provider = providerRepository.findByUser_Email(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return bookingRepository.findByProvider(provider);
    }

    // =========================================================
    // ADMIN ALL BOOKINGS
    // =========================================================

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    // =========================================================
    // PRICE PREVIEW
    // =========================================================

    public BookingPricePreviewResponse previewBookingPrice(BookingPreviewRequestDTO request) {

        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        double total = 0;
        Map<String, Double> breakdown = new HashMap<>();

        ProviderAvailability slot = availabilityRepository.findById(request.getAvailabilityId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        for (BookingServiceRequestDTO s : request.getServices()) {
            ServiceType service = s.getServiceType();
            Integer hours       = s.getHours();

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(provider, service, provider.getCity())
                    .orElseThrow(() -> new RuntimeException("Pricing not found"));

            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                if (hours == null || hours <= 0) throw new RuntimeException("Hours per day required for hourly services");
            }

            double price = bookingCalculationService.calculatePrice(
                    pricing, service, request.getMembers(), request.getHouseSize(), hours
            );
            breakdown.put(service.name(), price);
            total += price;
        }

        return new BookingPricePreviewResponse(
                total, breakdown,
                provider.getUser().getName(),
                slot.getStartTime().toString(),
                slot.getEndTime().toString(),
                request.getHouseSize(),
                request.getMembers()
        );
    }

    public void autoCompleteBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.SERVICE_IN_PROGRESS) {
            if (booking.getWorkStartDate() != null &&
                    booking.getWorkStartDate().plusDays(SERVICE_DAYS).isBefore(LocalDate.now())) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setWorkEndDate(booking.getWorkStartDate().plusDays(SERVICE_DAYS));
            }
        }
    }

    // =========================================================
    // GET PROVIDER OPTIONS — 3-tier geo matching
    // =========================================================

    public List<ProviderOptionDTO> getProviderOptions(BookingPreviewRequestDTO request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userCity    = user.getCity();
        String userPincode = user.getPincode();
        Double userLat     = user.getLatitude();
        Double userLng     = user.getLongitude();

        Map<Long, ProviderOptionDTO> candidates = new LinkedHashMap<>();

        if (userPincode != null && !userPincode.isBlank()) {
            for (Provider provider : providerRepository.findVerifiedByServiceablePincode(userPincode)) {
                ProviderOptionDTO dto = buildProviderDTO(provider, request, userCity, userLat, userLng, "PINCODE_MATCH");
                if (dto != null) candidates.put(provider.getId(), dto);
            }
        }

        if (userLat != null && userLng != null) {
            for (Provider provider : providerRepository.findVerifiedWithGeoInCity(userCity)) {
                if (candidates.containsKey(provider.getId())) continue;
                double radius = provider.getTravelRadiusKm() != null ? provider.getTravelRadiusKm() : 10.0;
                if (GeoUtil.isWithinRadius(provider.getHomeLatitude(), provider.getHomeLongitude(), userLat, userLng, radius)) {
                    ProviderOptionDTO dto = buildProviderDTO(provider, request, userCity, userLat, userLng, "RADIUS_MATCH");
                    if (dto != null) candidates.put(provider.getId(), dto);
                }
            }
        }

        if (candidates.isEmpty()) {
            for (Provider provider : providerRepository.findVerifiedWillingToTravelInCity(userCity)) {
                ProviderOptionDTO dto = buildProviderDTO(provider, request, userCity, userLat, userLng, "CITY_MATCH");
                if (dto != null) candidates.put(provider.getId(), dto);
            }
        }

        List<ProviderOptionDTO> result = new ArrayList<>(candidates.values());
        result.sort(Comparator.comparingDouble(dto -> -scoreProvider(dto)));
        return result;
    }

    private ProviderOptionDTO buildProviderDTO(Provider provider, BookingPreviewRequestDTO request,
                                               String userCity, Double userLat, Double userLng, String matchReason) {
        List<ProviderAvailability> slots = availabilityRepository.findByProvider(provider);
        boolean hasValidSlot = slots.stream().anyMatch(slot ->
                slot.isActive() && slot.getDate().isEqual(request.getStartDate())
        );
        if (!hasValidSlot) return null;

        double total = 0;
        Map<String, Double> breakdown = new HashMap<>();

        for (BookingServiceRequestDTO s : request.getServices()) {
            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(provider, s.getServiceType(), userCity)
                    .orElse(null);
            if (pricing == null) return null;

            Integer hours = s.getHours();
            if (hours == null || hours <= 0) hours = 1;

            double price = bookingCalculationService.calculatePrice(
                    pricing, s.getServiceType(), request.getMembers(), request.getHouseSize(), hours
            );
            breakdown.put(s.getServiceType().name(), price);
            total += price;
        }

        double distanceKm = -1;
        if (userLat != null && userLng != null
                && provider.getHomeLatitude() != null
                && provider.getHomeLongitude() != null) {
            distanceKm = GeoUtil.distanceKm(
                    provider.getHomeLatitude(), provider.getHomeLongitude(), userLat, userLng
            );
        }

        return new ProviderOptionDTO(
                provider.getId(),
                provider.getUser().getName(),
                provider.getRating(),
                provider.getExperienceYears(),
                total,
                breakdown,
                provider.getProfilePhotoUrl(),
                distanceKm,
                matchReason
        );
    }

    private double scoreProvider(ProviderOptionDTO dto) {
        double score = 0;
        switch (dto.getMatchReason()) {
            case "PINCODE_MATCH" -> score += 30;
            case "RADIUS_MATCH"  -> score += 20;
            case "CITY_MATCH"    -> score += 5;
        }
        score += (dto.getRating() / 5.0) * 25;
        if (dto.getDistanceKm() >= 0) {
            score += Math.max(0, 20 - (dto.getDistanceKm() / 50.0) * 20);
        }
        score += Math.min(dto.getExperienceYears(), 10);
        return score;
    }
}