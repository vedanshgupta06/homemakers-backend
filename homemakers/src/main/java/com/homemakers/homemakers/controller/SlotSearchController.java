package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.AvailableSlotResponse;
import com.homemakers.homemakers.service.SlotSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotSearchController {

    private final SlotSearchService slotSearchService;

    public SlotSearchController(SlotSearchService slotSearchService) {
        this.slotSearchService = slotSearchService;
    }

    @GetMapping("api/available")
    @PreAuthorize("hasRole('USER')")
    public List<AvailableSlotResponse> getAvailableSlots(

            @RequestParam LocalDate date,
            @RequestParam int durationMinutes
    ) {

        return slotSearchService.findAvailableSlots(
                date,
                durationMinutes
        );
    }
}