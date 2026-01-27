package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.WeeklySummaryResponse;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class ProviderPayoutTransactionService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final BookingRepository bookingRepository;
    private final ProviderPayoutTransactionRepository transactionRepository;

    public ProviderPayoutTransactionService(
            UserRepository userRepository,
            ProviderRepository providerRepository,
            BookingRepository bookingRepository,
            ProviderPayoutTransactionRepository transactionRepository
    ) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.bookingRepository = bookingRepository;
        this.transactionRepository = transactionRepository;
    }

    // ===============================
    // WEEKLY SUMMARY
    // ===============================
    @Transactional
    public List<WeeklySummaryResponse> getWeeklySummary(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        String payoutMonth = YearMonth.now().toString();

        List<Booking> bookings =
                bookingRepository.findByProviderAndStatus(
                        provider, BookingStatus.COMPLETED
                );

        List<WeeklySummaryResponse> response = new ArrayList<>();

        for (Booking booking : bookings) {

            double monthlyAmount = booking.getTotalPrice();
            double weeklyAmount = monthlyAmount / 4;

            List<ProviderPayoutTransaction> txns =
                    transactionRepository.findByProviderAndBookingAndPayoutMonth(
                            provider, booking, payoutMonth
                    );

            Map<Integer, ProviderPayoutTransaction> map = new HashMap<>();
            for (ProviderPayoutTransaction t : txns) {
                map.put(t.getWeekNo(), t);
            }

            List<WeeklySummaryResponse.WeekStatus> weeks = new ArrayList<>();

            for (int week = 1; week <= 4; week++) {

                ProviderPayoutTransaction txn = map.get(week);

                if (txn == null) {
                    txn = new ProviderPayoutTransaction();
                    txn.setProvider(provider);
                    txn.setBooking(booking);
                    txn.setPayoutMonth(payoutMonth);
                    txn.setWeekNo(week);
                    txn.setAmount(weeklyAmount);
                    txn.setStatus(WeeklyPayoutStatus.AVAILABLE);
                    txn.setRequestedAt(LocalDateTime.now());
                    txn = transactionRepository.save(txn);
                }

                weeks.add(new WeeklySummaryResponse.WeekStatus(
                        week,
                        txn.getAmount(),
                        txn.getStatus().name()
                ));
            }

            for (ServiceType service : booking.getServices()) {
                response.add(new WeeklySummaryResponse(
                        booking.getId(),
                        service.name(),
                        monthlyAmount,
                        weeklyAmount,
                        weeks
                ));
            }
        }

        return response;
    }

    // ===============================
    // REQUEST WEEKLY PAYOUT
    // ===============================
    @Transactional
    public ProviderPayoutTransaction requestPayout(
            String email,
            Long bookingId,
            int weekNo,
            double amount
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 🔒 Security & lifecycle checks
        if (!booking.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("Unauthorized booking access");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Booking not completed yet");
        }

        String payoutMonth = YearMonth.now().toString();

        ProviderPayoutTransaction txn =
                transactionRepository
                        .findByProviderAndBookingAndPayoutMonthAndWeekNo(
                                provider,
                                booking,
                                payoutMonth,
                                weekNo
                        )
                        .orElseThrow(() -> new RuntimeException("Week not found"));

        // ✅ Only AVAILABLE → REQUESTED allowed
        if (txn.getStatus() != WeeklyPayoutStatus.AVAILABLE) {
            throw new RuntimeException("Week already requested or paid");
        }

        double allowedWeekly = booking.getTotalPrice() / 4;
        if (amount > allowedWeekly) {
            throw new RuntimeException("Amount exceeds weekly limit");
        }

        txn.setStatus(WeeklyPayoutStatus.REQUESTED);
        txn.setRequestedAt(LocalDateTime.now());

        return transactionRepository.save(txn);
    }
}