package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.PaymentPreviewRequest;
import com.homemakers.homemakers.dto.PaymentPreviewResponse;
import com.homemakers.homemakers.service.PaymentPreviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('USER')")
public class PaymentPreviewController {

    private final PaymentPreviewService previewService;

    public PaymentPreviewController(PaymentPreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/preview")
    public PaymentPreviewResponse preview(
            @RequestBody PaymentPreviewRequest request
    ) {
        return previewService.preview(request);
    }
}
