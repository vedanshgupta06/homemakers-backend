package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.PaymentPreviewRequest;
import com.homemakers.homemakers.dto.PaymentPreviewResponse;
import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.repository.BookingRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentPreviewService {

    private static final double PLATFORM_FEE_FLAT = 199;
    private static final double SUBSCRIPTION_PERCENT = 0.10;
    private static final double SUBSCRIPTION_MIN_AMOUNT = 5000;

    private final BookingRepository bookingRepository;

    public PaymentPreviewService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public PaymentPreviewResponse preview(PaymentPreviewRequest request) {

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        double serviceAmount = booking.getTotalPrice();

        boolean subscriptionAllowed = serviceAmount >= SUBSCRIPTION_MIN_AMOUNT;
        boolean subscriptionTaken =
                subscriptionAllowed && request.isSubscriptionTaken();

        double platformFee = subscriptionTaken
                ? serviceAmount * SUBSCRIPTION_PERCENT
                : PLATFORM_FEE_FLAT;

        double totalPayable = serviceAmount + platformFee;

        return new PaymentPreviewResponse(
                serviceAmount,
                platformFee,
                subscriptionAllowed,
                subscriptionTaken,
                totalPayable
        );
    }
}
