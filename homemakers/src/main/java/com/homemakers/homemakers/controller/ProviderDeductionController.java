package com.homemakers.homemakers.controller;


import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderDeduction;
import com.homemakers.homemakers.service.deduction.ProviderDeductionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/provider/deductions")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderDeductionController {

    private final ProviderDeductionService deductionService;

    public ProviderDeductionController(ProviderDeductionService deductionService) {
        this.deductionService = deductionService;
    }

    @GetMapping
    public List<ProviderDeduction> getMyDeductions(Principal principal) {
        return deductionService.getDeductionsForProvider(principal.getName());
    }
}
