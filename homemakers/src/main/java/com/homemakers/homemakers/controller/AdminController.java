package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.DeductionRequest;
import com.homemakers.homemakers.model.ProviderDeduction;
import com.homemakers.homemakers.service.AdminDeductionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDeductionService adminDeductionService;

    public AdminController(AdminDeductionService adminDeductionService) {
        this.adminDeductionService = adminDeductionService;
    }


}
