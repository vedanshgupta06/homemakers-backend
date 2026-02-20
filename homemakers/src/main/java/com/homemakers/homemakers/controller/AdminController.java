package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Booking;
import com.homemakers.homemakers.repository.BookingRepository;
import com.homemakers.homemakers.service.AdminDeductionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminDeductionService adminDeductionService;
    private final BookingRepository bookingRepository;

    public AdminController(
            AdminDeductionService adminDeductionService,
            BookingRepository bookingRepository
    ) {
        this.adminDeductionService = adminDeductionService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/bookings")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}
