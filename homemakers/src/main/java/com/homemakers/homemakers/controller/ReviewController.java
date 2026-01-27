package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.ReviewRequest;
import com.homemakers.homemakers.model.Review;
import com.homemakers.homemakers.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/booking/{bookingId}")
    @PreAuthorize("hasRole('USER')")
    public Review addReview(
            @PathVariable Long bookingId,
            @RequestBody ReviewRequest request
    ) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return reviewService.addReview(bookingId, request, email);
    }
}
