package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.service.AdminDeductionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDeductionService adminDeductionService;
    private final BookingRepository bookingRepository;
    private final ProviderRepository providerRepository;
    public AdminController(
            AdminDeductionService adminDeductionService,
            BookingRepository bookingRepository,
            ProviderRepository providerRepository
    ) {
        this.adminDeductionService = adminDeductionService;
        this.bookingRepository = bookingRepository;
        this.providerRepository = providerRepository;
    }

    @GetMapping("/bookings")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    @GetMapping("/providers")
    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }
}
