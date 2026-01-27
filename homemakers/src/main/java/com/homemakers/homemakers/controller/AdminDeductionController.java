package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.AdminNoteRequest;
import com.homemakers.homemakers.dto.DeductionRequest;
import com.homemakers.homemakers.model.ProviderDeduction;
import com.homemakers.homemakers.service.AdminDeductionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/deductions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeductionController {

    private final AdminDeductionService service;

    public AdminDeductionController(AdminDeductionService service) {
        this.service = service;
    }

    @PostMapping("/{id}/approve")
    public ProviderDeduction approve(
            @PathVariable Long id,
            @RequestBody AdminNoteRequest request
    ) {
        return service.approveDeduction(id, request.getNote());
    }

    @PostMapping("/{id}/waive")
    public ProviderDeduction waive(
            @PathVariable Long id,
            @RequestBody AdminNoteRequest request
    ) {
        return service.waiveDeduction(id, request.getNote());
    }
}
