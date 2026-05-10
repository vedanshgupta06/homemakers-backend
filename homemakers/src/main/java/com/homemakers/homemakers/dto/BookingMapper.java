package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BookingMapper {

    public Map<String, Object> toMap(Booking b) {
        Map<String, Object> map = new HashMap<>();

        // Scalar fields
        map.put("id",                  b.getId());
        map.put("status",              b.getStatus() != null ? b.getStatus().name() : null);
        map.put("paymentStatus",       b.getPaymentStatus() != null ? b.getPaymentStatus().name() : null);
        map.put("totalPrice",          b.getTotalPrice());           // base service price — provider earns this
        map.put("platformFee",         b.getPlatformFee());          // 5% platform cut
        map.put("totalWithFee",        b.getTotalWithFee());         // totalPrice + platformFee — user pays this
        map.put("finalPayableAmount",  b.getFinalPayableAmount());   // after wallet deduction
        map.put("walletUsed",          b.getWalletUsed());
        map.put("walletEligible",      b.getWalletEligible());
        map.put("walletConsentStatus", b.getWalletConsentStatus() != null ? b.getWalletConsentStatus().name() : null);
        map.put("bookingStartTime",    b.getBookingStartTime() != null ? b.getBookingStartTime().toString() : null);
        map.put("bookingEndTime",      b.getBookingEndTime() != null ? b.getBookingEndTime().toString() : null);
        map.put("originalSlotStart",   b.getOriginalSlotStart() != null ? b.getOriginalSlotStart().toString() : null);
        map.put("originalSlotEnd",     b.getOriginalSlotEnd() != null ? b.getOriginalSlotEnd().toString() : null);
        map.put("hoursPerDay",         b.getHoursPerDay());
        map.put("createdAt",           b.getCreatedAt());
        map.put("updatedAt",           b.getUpdatedAt());
        map.put("completedAt",         b.getCompletedAt());
        map.put("workStartDate",       b.getWorkStartDate());
        map.put("workEndDate",         b.getWorkEndDate());
        map.put("penaltyApplied",      b.getPenaltyApplied());
        map.put("terminationReason",   b.getTerminationReason());
        map.put("settlementDone",      b.isSettlementDone());
        map.put("customerNote",        b.getCustomerNote());
        map.put("stripeSessionId",     b.getStripeSessionId());
        map.put("stripePaymentIntent", b.getStripePaymentIntent());
        map.put("holidays",            b.getHolidays());
        map.put("totalDays",           b.getTotalDays());
        map.put("chargeableDays",      b.getChargeableDays());

        // Lazy relations
        map.put("serviceDate",      b.getAvailability() != null ? b.getAvailability().getDate().toString() : null);
        map.put("services",         b.getServices() != null ? b.getServices().stream().map(Enum::name).toList() : List.of());
        map.put("providerName",     b.getProvider() != null && b.getProvider().getUser() != null ? b.getProvider().getUser().getName() : null);
        map.put("providerCity",     b.getProvider() != null ? b.getProvider().getCity() : null);
        map.put("providerRating",   b.getProvider() != null ? b.getProvider().getRating() : null);
        map.put("providerVerified", b.getProvider() != null && b.getProvider().isVerified());
        map.put("providerPhoto",    b.getProvider() != null ? b.getProvider().getProfilePhotoUrl() : null);

        return map;
    }

    public Map<String, Object> toMap(Booking b, int totalDays, int chargeableDays,
                                     int absent, int leave, boolean rated) {
        Map<String, Object> map = toMap(b);
        map.put("totalDays",      totalDays);
        map.put("chargeableDays", chargeableDays);
        map.put("absent",         absent);
        map.put("leave",          leave);
        map.put("rated",          rated);
        return map;
    }
}