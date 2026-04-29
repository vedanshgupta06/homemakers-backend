package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.ComplaintRequest;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.model.event.ComplaintEvent;
import com.homemakers.homemakers.model.event.ComplaintSeverity;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.service.deduction.ComplaintDeductionProcessor;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ComplaintDeductionProcessor complaintDeductionProcessor;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ComplaintDeductionProcessor complaintDeductionProcessor
    ) {
        this.complaintRepository = complaintRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.complaintDeductionProcessor = complaintDeductionProcessor;
    }

    // ── USER: Submit complaint ──
    @Transactional
    public Complaint submitComplaint(ComplaintRequest request, String userEmail) {

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Can only complain about completed bookings");
        }

        if (complaintRepository.existsByBookingIdAndStatus(
                booking.getId(), ComplaintStatus.PENDING)) {
            throw new RuntimeException("A complaint is already pending for this booking");
        }

        ComplaintSeverity severity = ComplaintSeverity.valueOf(
                request.getSeverity().toUpperCase()
        );

        Complaint complaint = new Complaint();
        complaint.setUser(booking.getUser());
        complaint.setProvider(booking.getProvider());
        complaint.setBooking(booking);
        complaint.setSeverity(severity);
        complaint.setDescription(request.getDescription());
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setCreatedAt(LocalDateTime.now());

        return complaintRepository.save(complaint);
    }

    // ── USER: Get my complaints ──
    public List<Complaint> getMyComplaints(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return complaintRepository.findByUserId(user.getId());
    }

    // ── ADMIN: Get all pending complaints ──
    public List<Complaint> getPendingComplaints() {
        return complaintRepository.findByStatus(ComplaintStatus.PENDING);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // ── ADMIN: Validate → triggers deduction ──
    @Transactional
    public Complaint validateComplaint(Long complaintId, String adminNote) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new RuntimeException("Complaint already resolved");
        }

        complaint.setStatus(ComplaintStatus.VALIDATED);
        complaint.setAdminNote(adminNote);
        complaint.setResolvedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        // Now trigger deduction
        ComplaintEvent event = new ComplaintEvent(
                complaint.getId(),
                complaint.getProvider().getId(),
                complaint.getBooking().getId(),
                complaint.getSeverity(),
                true
        );

        complaintDeductionProcessor.process(event);

        return complaint;
    }

    // ── ADMIN: Reject complaint ──
    @Transactional
    public Complaint rejectComplaint(Long complaintId, String adminNote) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new RuntimeException("Complaint already resolved");
        }

        complaint.setStatus(ComplaintStatus.REJECTED);
        complaint.setAdminNote(adminNote);
        complaint.setResolvedAt(LocalDateTime.now());

        return complaintRepository.save(complaint);
    }
}