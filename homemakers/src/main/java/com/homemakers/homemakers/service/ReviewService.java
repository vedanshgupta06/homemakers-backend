package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.ReviewRequest;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.model.event.ComplaintEvent;
import com.homemakers.homemakers.model.event.ComplaintSeverity;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.service.deduction.ComplaintDeductionProcessor;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ComplaintDeductionProcessor complaintDeductionProcessor;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ProviderRepository providerRepository,
            ComplaintDeductionProcessor complaintDeductionProcessor
    ) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.complaintDeductionProcessor = complaintDeductionProcessor;
    }

    @Transactional
    public Review addReview(
            Long bookingId,
            ReviewRequest request,
            String userEmail
    ) {

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Booking not completed");
        }

        if (reviewRepository.existsByBooking_Id(bookingId)) {
            throw new RuntimeException("Review already exists");
        }

        User user = booking.getUser();
        Provider provider = booking.getProvider();

        Review review = new Review();
        review.setUser(user);
        review.setProvider(provider);
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        // ⭐ Update provider rating
        provider.setTotalRatings(provider.getTotalRatings() + 1);
        provider.setRating(
                (provider.getRating() * (provider.getTotalRatings() - 1)
                        + request.getRating())
                        / provider.getTotalRatings()
        );

        providerRepository.save(provider);
        Review savedReview = reviewRepository.save(review);

        // 🔥 AUTOMATIC COMPLAINT → DEDUCTION FLOW
        ComplaintSeverity severity;
        if (savedReview.getRating() <= 2) {
            severity = ComplaintSeverity.HIGH;
        } else if (savedReview.getRating() == 3) {
            severity = ComplaintSeverity.MEDIUM;
        } else {
            severity = ComplaintSeverity.LOW;
        }

        ComplaintEvent event = new ComplaintEvent(
                savedReview.getId(),               // complaintId
                provider.getId(),                  // providerId
                booking.getId(),                   // bookingId
                severity,
                true                               // validated (MVP)
        );

        complaintDeductionProcessor.process(event);

        return savedReview;
    }
}
