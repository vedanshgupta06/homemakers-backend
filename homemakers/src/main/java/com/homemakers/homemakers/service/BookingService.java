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

        LocalTime requestStart = slot.getStartTime();
        LocalTime requestEnd   = requestStart.plusMinutes(requiredMinutes);

        if (requestEnd.isAfter(slot.getEndTime())) {
            throw new RuntimeException("Requested time outside provider slot");
        }

        final int BUFFER_MINUTES = 15;

        LocalTime originalSlotStart = slot.getStartTime();
        LocalTime originalSlotEnd   = slot.getEndTime();
        LocalDate slotDate          = slot.getDate();

        availabilityRepository.delete(slot);
        availabilityRepository.flush();

        // 1. Booked slot — inactive, exact booking window
        ProviderAvailability bookedSlot = new ProviderAvailability();
        bookedSlot.setProvider(provider);
        bookedSlot.setDate(slotDate);
        bookedSlot.setStartTime(requestStart);
        bookedSlot.setEndTime(requestEnd);
        bookedSlot.setActive(false);
        ProviderAvailability savedBookedSlot = availabilityRepository.save(bookedSlot);

        // 2. Before-slot — active, only if booking starts after slot start
        if (requestStart.isAfter(originalSlotStart)) {
            ProviderAvailability beforeSlot = new ProviderAvailability();
            beforeSlot.setProvider(provider);
            beforeSlot.setDate(slotDate);
            beforeSlot.setStartTime(originalSlotStart);
            beforeSlot.setEndTime(requestStart);
            beforeSlot.setActive(true);
            availabilityRepository.save(beforeSlot);
        }

        // 3. After-slot — active, only if time remains after booking + buffer
        LocalTime bufferedEnd = requestEnd.plusMinutes(BUFFER_MINUTES);
        if (bufferedEnd.isBefore(originalSlotEnd)) {
            ProviderAvailability afterSlot = new ProviderAvailability();
            afterSlot.setProvider(provider);
            afterSlot.setDate(slotDate);
            afterSlot.setStartTime(bufferedEnd);
            afterSlot.setEndTime(originalSlotEnd);
            afterSlot.setActive(true);
            availabilityRepository.save(afterSlot);
        }

        Booking booking = new Booking();
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUser(user);
        booking.setProvider(provider);
        booking.setAvailability(savedBookedSlot);
        booking.setServices(services);
        booking.setTotalPrice(totalMonthlyPrice);
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
        double walletEligible  = Math.min(availableWallet, totalMonthlyPrice);

        savedBooking.setWalletUsed(0.0);
        savedBooking.setWalletEligible(walletEligible);
        savedBooking.setFinalPayableAmount(totalMonthlyPrice);
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

        if (useWallet) {
            double reserved = userWalletService.reserveAmount(
                    booking.getUser().getId(),
                    booking.getId(),
                    booking.getTotalPrice()
            );
            booking.setWalletUsed(reserved);
            booking.setFinalPayableAmount(booking.getTotalPrice() - reserved);
            booking.setWalletConsentStatus(WalletConsentStatus.ACCEPTED);
        } else {
            booking.setWalletUsed(0.0);
            booking.setFinalPayableAmount(booking.getTotalPrice());
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

        // Only deduct wallet if user explicitly consented
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

        // ✅ Lock all future slots that overlap with this booking's time window
        // for the next 30 days (monthly service duration)
        lockOverlappingFutureSlots(booking);

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
        LocalTime bufferedEnd   = bookingEnd.plusMinutes(15);

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

        // ✅ Reactivate anchor slot + unlock all future locked slots
        reactivateSlotForBooking(booking);

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

        // ✅ Reactivate anchor slot + unlock all future locked slots
        reactivateSlotForBooking(booking);

        return booking;
    }

    // =========================================================
    // PRIVATE — Lock future overlapping slots on booking accept
    // When a booking is confirmed, find all of the provider's
    // ACTIVE slots in the next 30 days that overlap with the
    // booking time window and mark them inactive.
    // This prevents double-bookings for monthly services.
    // =========================================================
    private void lockOverlappingFutureSlots(Booking booking) {
        Provider provider   = booking.getProvider();
        LocalDate startDate = booking.getAvailability().getDate();
        LocalDate endDate   = startDate.plusDays(30);
        LocalTime bookStart = booking.getBookingStartTime();
        LocalTime bookEnd   = booking.getBookingEndTime();

        if (bookStart == null || bookEnd == null) return;

        List<ProviderAvailability> allSlots = availabilityRepository.findByProvider(provider);

        for (ProviderAvailability slot : allSlots) {

            // Only future slots within the 30-day window (skip anchor date)
            if (slot.getDate().isBefore(startDate.plusDays(1))) continue;
            if (slot.getDate().isAfter(endDate)) continue;

            // Only active slots
            if (!slot.isActive()) continue;

            // Overlap: slot starts before bookEnd AND slot ends after bookStart
            boolean overlaps = slot.getStartTime().isBefore(bookEnd)
                    && slot.getEndTime().isAfter(bookStart);

            if (overlaps) {
                slot.setActive(false);
                availabilityRepository.save(slot);
                System.out.println("🔒 Locked slot #" + slot.getId()
                        + " on " + slot.getDate()
                        + " " + slot.getStartTime() + "-" + slot.getEndTime()
                        + " due to booking #" + booking.getId());
            }
        }
    }

    // =========================================================
    // PRIVATE — Reactivate anchor slot + all future locked slots
    // When booking ends for any reason (completed, terminated,
    // or cancelled by scheduler), reactivate all slots that were
    // locked for the 30-day range.
    // =========================================================
    private void reactivateSlotForBooking(Booking booking) {
        if (booking.getAvailability() == null) return;

        Provider provider   = booking.getProvider();
        LocalDate startDate = booking.getAvailability().getDate();
        LocalTime bookStart = booking.getBookingStartTime();
        LocalTime bookEnd   = booking.getBookingEndTime();

        // Use actual work end date if available, otherwise estimate 30 days
        LocalDate endDate = booking.getWorkEndDate() != null
                ? booking.getWorkEndDate()
                : startDate.plusDays(30);

        // Reactivate the anchor slot itself
        ProviderAvailability anchorSlot = availabilityRepository
                .findById(booking.getAvailability().getId())
                .orElse(null);

        if (anchorSlot != null && !anchorSlot.isActive()) {
            anchorSlot.setActive(true);
            availabilityRepository.save(anchorSlot);
            System.out.println("🔓 Reactivated anchor slot #" + anchorSlot.getId());
        }

        if (bookStart == null || bookEnd == null) return;

        List<ProviderAvailability> allSlots = availabilityRepository.findByProvider(provider);

        for (ProviderAvailability slot : allSlots) {

            // Only slots in the booking's date range (skip anchor date)
            if (slot.getDate().isBefore(startDate.plusDays(1))) continue;
            if (slot.getDate().isAfter(endDate)) continue;

            // Only currently inactive slots
            if (slot.isActive()) continue;

            // Only slots overlapping the booking time window
            boolean overlaps = slot.getStartTime().isBefore(bookEnd)
                    && slot.getEndTime().isAfter(bookStart);

            if (overlaps) {
                // Safety check: don't reactivate if another booking is using this slot
                boolean hasOtherBooking = bookingRepository.existsByAvailability(slot);

                if (!hasOtherBooking) {
                    slot.setActive(true);
                    availabilityRepository.save(slot);
                    System.out.println("🔓 Reactivated slot #" + slot.getId()
                            + " on " + slot.getDate()
                            + " " + slot.getStartTime() + "-" + slot.getEndTime());
                }
            }
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
                    booking.getWorkStartDate().plusDays(30).isBefore(LocalDate.now())) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setWorkEndDate(booking.getWorkStartDate().plusDays(30));
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