
package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.*;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.util.ServiceDurationUtil;
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
            PaymentTransactionRepository paymentTransactionRepository
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
    }

    // =========================================================
    // CREATE BOOKING
    // =========================================================

    @Transactional
    public Booking createBooking(BookingPreviewRequestDTO request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String userCity = user.getCity();
        ProviderAvailability slot =
                availabilityRepository.findWithLockById(request.getAvailabilityId())
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.isActive()) {
            throw new RuntimeException("Slot already booked");
        }

        Provider provider = slot.getProvider();

        int requiredMinutes = 0;
        double totalMonthlyPrice = 0;

        Set<ServiceType> services = new HashSet<>();

        for (BookingServiceRequestDTO s : request.getServices()) {

            ServiceType type = s.getServiceType();
            services.add(type);

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(
                            provider,
                            type,
                            provider.getCity()
                    )
                    .orElseThrow(() ->
                            new RuntimeException("Pricing not set for " + type)
                    );

            Integer hours = s.getHours();

            // PRICE CALCULATION
            double price = bookingCalculationService.calculatePrice(
                    pricing,
                    type,
                    request.getMembers(),
                    request.getHouseSize(),
                    hours
            );

            totalMonthlyPrice += price;

            // TIME CALCULATION
            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                requiredMinutes += hours * 60;
            } else {
                requiredMinutes += ServiceDurationUtil.getMinutes(type);
            }
        }

        LocalTime requestStart = slot.getStartTime();
        LocalTime requestEnd = requestStart.plusMinutes(requiredMinutes);

        if (requestEnd.isAfter(slot.getEndTime())) {
            throw new RuntimeException("Requested time outside provider slot");
        }

        Booking booking = new Booking();
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUser(user);
        booking.setProvider(provider);
        booking.setAvailability(slot);
        booking.setServices(services);

        double totalPrice = totalMonthlyPrice;

        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);

        bookingRepository.save(booking);

// 🆕 Reserve wallet instead of deducting
        double walletReserved = userWalletService.reserveAmount(
                user.getId(),
                booking.getId(),
                totalPrice
        );

        booking.setWalletUsed(walletReserved);
        booking.setFinalPayableAmount(totalPrice - walletReserved);
        booking.setBookingStartTime(requestStart);
        booking.setBookingEndTime(requestEnd);

        bookingRepository.save(booking);

        // SLOT SPLITTING
// 🔥 BUFFER TIME
        int BUFFER_MINUTES = 15;

        LocalTime slotStart = slot.getStartTime();
        LocalTime slotEnd = slot.getEndTime();

// 1️⃣ CASE: booking starts at slot start
        if (requestStart.equals(slotStart)) {

            LocalTime bufferedEnd = requestEnd.plusMinutes(BUFFER_MINUTES);

            if (bufferedEnd.isBefore(slotEnd)) {

                slot.setStartTime(bufferedEnd); // shift slot forward
                availabilityRepository.save(slot);

            } else {
                slot.setActive(false); // fully consumed
                availabilityRepository.save(slot);
            }
        }

// 2️⃣ CASE: booking in middle
        else {

            // 🔹 BEFORE SLOT (available)
            slot.setEndTime(requestStart);
            availabilityRepository.save(slot);

            // 🔹 AFTER SLOT (with buffer)
            LocalTime bufferedEnd = requestEnd.plusMinutes(BUFFER_MINUTES);

            if (bufferedEnd.isBefore(slotEnd)) {

                ProviderAvailability afterSlot = new ProviderAvailability();
                afterSlot.setProvider(provider);
                afterSlot.setDate(slot.getDate());
                afterSlot.setStartTime(bufferedEnd);
                afterSlot.setEndTime(slotEnd);
                afterSlot.setActive(true);

                availabilityRepository.save(afterSlot);
            }
        }
        return booking;
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

        // 🆕 CONFIRM RESERVED AMOUNT (VERY IMPORTANT)
        if (booking.getWalletUsed() != null && booking.getWalletUsed() > 0) {

            userWalletService.confirmReservedAmount(
                    booking.getUser().getId(),
                    booking.getId(),
                    booking.getWalletUsed()
            );
        }
// 🔥 SAVE WALLET PAYMENT TRANSACTION
        if (booking.getWalletUsed() != null && booking.getWalletUsed() > 0) {

            PaymentTransaction txn = new PaymentTransaction();
            txn.setUserId(booking.getUser().getId());
            txn.setBookingId(booking.getId());
            txn.setAmount(booking.getWalletUsed());
            txn.setMethod(PaymentMethod.WALLET);
            txn.setStatus(PaymentStatus.PAID);
            txn.setDescription("Paid via wallet");

            paymentTransactionRepository.save(txn);
        }
        // 🆕 SET PAYMENT STATUS CORRECTLY
        if (booking.getFinalPayableAmount() != null &&
                booking.getFinalPayableAmount() == 0) {

            paymentService.markBookingAsPaid(booking); // ✅ FIX

        } else {
            booking.setPaymentStatus(PaymentStatus.PAYMENT_REQUIRED);
        }

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

        // ✅ Payment check
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Payment not completed");
        }

        // ✅ Status check
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Booking not confirmed");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = booking.getAvailability().getDate();

        // 🔥 PREVENT EARLY START
        if (today.isBefore(startDate)) {
            throw new RuntimeException("Work cannot be started before the scheduled date");
        }

        // 🔥 PREVENT LATE START
        if (today.isAfter(startDate)) {
            throw new RuntimeException("Start date has already passed. Contact admin.");
        }

        // ✅ Start work
        booking.markWorkStarted(today);
        booking.setStatus(BookingStatus.SERVICE_IN_PROGRESS);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // PROVIDER END WORK
    // =========================================================

//    @Transactional
//    public Booking endWork(Long bookingId, String providerEmail) {
//
//        Booking booking = bookingRepository
//                .findByIdAndProvider_User_Email(bookingId, providerEmail)
//                .orElseThrow(() -> new RuntimeException("Booking not found"));
//
//        if (booking.getStatus() != BookingStatus.SERVICE_IN_PROGRESS) {
//            throw new IllegalStateException("Work not in progress");
//        }
//
//        booking.markWorkEnded(LocalDate.now());
//        booking.setStatus(BookingStatus.SERVICE_DONE);
//
//        return bookingRepository.save(booking);
//    }

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

        // 🆕 RELEASE RESERVED MONEY
        if (booking.getWalletUsed() != null && booking.getWalletUsed() > 0) {

            userWalletService.releaseReservedAmount(
                    booking.getUser().getId(),
                    booking.getId(),
                    booking.getWalletUsed()
            );
        }

        ProviderAvailability availability = booking.getAvailability();
        availability.setActive(true);

        availabilityRepository.save(availability);

        return bookingRepository.save(booking);
    }
    // =========================================================
    // ADMIN STOP SERVICE
    // =========================================================

//    @Transactional
//    public Booking stopWorkEarly(Long bookingId) {
//
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new RuntimeException("Booking not found"));
//
//        booking.markWorkEnded(LocalDate.now());
//        booking.setStatus(BookingStatus.SERVICE_STOPPED_BY_ADMIN);
//
//        return bookingRepository.save(booking);
//    }

    // =========================================================
    // ADMIN COMPLETE BOOKING
    // =========================================================

    @Transactional
    public Booking finalizeBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());

        // ✅ ADD THIS LINE
        providerSettlementService.finalizeSettlement(booking);

        return bookingRepository.save(booking);
    }
    @Transactional
    public Booking terminateBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.SERVICE_IN_PROGRESS) {
            throw new RuntimeException("Only active bookings can be terminated");
        }

        booking.setStatus(BookingStatus.TERMINATED);
        booking.setWorkEndDate(LocalDate.now());
        booking.setCompletedAt(LocalDateTime.now());

        bookingRepository.save(booking);

        // ✅ ADD THIS (MAIN FIX)
        if (!booking.isSettlementDone()) {
            providerSettlementService.finalizeSettlement(booking);
            booking.setSettlementDone(true);
            bookingRepository.save(booking);
        }

        return booking;
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

        Provider provider = providerRepository
                .findByUser_Email(providerEmail)
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

    public BookingPricePreviewResponse previewBookingPrice(
            BookingPreviewRequestDTO request
    ) {

        Provider provider = providerRepository
                .findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        double total = 0;
        Map<String, Double> breakdown = new HashMap<>();

        ProviderAvailability slot =
                availabilityRepository.findById(request.getAvailabilityId())
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

        for (BookingServiceRequestDTO s : request.getServices()) {

            ServiceType service = s.getServiceType();
            Integer hours = s.getHours();

            ServicePricing pricing = pricingRepository
                    .findByProviderAndServiceAndCity(
                            provider,
                            service,
                            provider.getCity()
                    )
                    .orElseThrow(() -> new RuntimeException("Pricing not found"));

            if (pricing.getPricingType() == PricingType.HOURLY_MONTHLY) {
                if (hours == null || hours <= 0) {
                    throw new RuntimeException("Hours per day required for hourly services");
                }
            }
            System.out.println("SERVICE = " + service +
                    " HOURS = " + hours +
                    " TYPE = " + pricing.getPricingType());
            double price = bookingCalculationService.calculatePrice(
                    pricing,
                    service,
                    request.getMembers(),
                    request.getHouseSize(),
                    hours
            );

            breakdown.put(service.name(), price);
            total += price;
        }
        return new BookingPricePreviewResponse(
                total,
                breakdown,
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
                    booking.getWorkStartDate().plusDays(30).isBefore(java.time.LocalDate.now())) {

                booking.setStatus(BookingStatus.COMPLETED);
                booking.setWorkEndDate(booking.getWorkStartDate().plusDays(30));
            }
        }
    }
    public List<ProviderOptionDTO> getProviderOptions(BookingPreviewRequestDTO request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userCity = user.getCity();

        List<Provider> providers = providerRepository.findByCity(userCity);

        List<ProviderOptionDTO> result = new ArrayList<>();

        for (Provider provider : providers) {


            List<ProviderAvailability> slots =
                    availabilityRepository.findByProvider(provider);

            boolean hasValidSlot = slots.stream().anyMatch(slot ->
                    slot.isActive() &&
                            slot.getDate().isEqual(request.getStartDate())
            );


            if (!hasValidSlot) {
                continue;
            }

            boolean validProvider = true;
            double total = 0;
            Map<String, Double> breakdown = new HashMap<>();

            for (BookingServiceRequestDTO s : request.getServices()) {

                ServicePricing pricing = pricingRepository
                        .findByProviderAndServiceAndCity(
                                provider,
                                s.getServiceType(),
                                userCity
                        )
                        .orElse(null);

                if (pricing == null) {
                    validProvider = false;
                    break;
                }

                double price = bookingCalculationService.calculatePrice(
                        pricing,
                        s.getServiceType(),
                        request.getMembers(),
                        request.getHouseSize(),
                        s.getHours()
                );

                breakdown.put(s.getServiceType().name(), price);
                total += price;
            }

            if (!validProvider) continue;

            result.add(new ProviderOptionDTO(
                    provider.getId(),
                    provider.getUser().getName(),
                    provider.getRating(),
                    provider.getExperienceYears(),
                    total,
                    breakdown,
                    provider.getProfilePhotoUrl()
            ));
        }
        return result;
    }
}
