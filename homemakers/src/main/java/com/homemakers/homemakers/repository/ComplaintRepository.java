package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Complaint;
import com.homemakers.homemakers.model.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStatus(ComplaintStatus status);
    List<Complaint> findByUserId(Long userId);
    boolean existsByBookingIdAndStatus(Long bookingId, ComplaintStatus status);
}