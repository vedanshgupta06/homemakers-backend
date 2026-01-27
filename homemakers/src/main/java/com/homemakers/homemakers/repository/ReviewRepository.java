package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBooking_Id(Long bookingId);
}
