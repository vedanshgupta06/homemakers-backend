package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.ProviderAutoDeductionService;
import com.homemakers.homemakers.service.ProviderLeaveSettlementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/provider/test")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderTestController {

    private final ProviderLeaveSettlementService leaveSettlementService;
    private final ProviderAutoDeductionService autoDeductionService;
    private final ProviderRepository providerRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ProviderTestController(
            ProviderLeaveSettlementService leaveSettlementService,
            ProviderAutoDeductionService autoDeductionService,
            ProviderRepository providerRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository
    ) {
        this.leaveSettlementService = leaveSettlementService;
        this.autoDeductionService = autoDeductionService;
        this.providerRepository = providerRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/settle/{bookingId}")
    public String testSettlement(
            @PathVariable Long bookingId,
            Principal principal
    ) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow();

        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow();

        leaveSettlementService.settleLeaves(provider, booking);
        autoDeductionService.applyUnpaidLeaveDeductions(provider, booking);

        return "Leave settlement + auto deduction executed";
    }
}
