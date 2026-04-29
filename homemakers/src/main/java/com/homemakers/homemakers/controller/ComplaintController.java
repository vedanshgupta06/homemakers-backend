package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.ComplaintRequest;
import com.homemakers.homemakers.model.Complaint;
import com.homemakers.homemakers.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // USER — submit
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Complaint> submit(@RequestBody ComplaintRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(complaintService.submitComplaint(request, email));
    }

    // USER — my complaints
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Complaint>> myComplaints() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(complaintService.getMyComplaints(email));
    }

    // ADMIN — all complaints
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Complaint>> all() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // ADMIN — pending only
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Complaint>> pending() {
        return ResponseEntity.ok(complaintService.getPendingComplaints());
    }

    // ADMIN — validate
    @PostMapping("/admin/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Complaint> validate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(
                complaintService.validateComplaint(id, body.get("adminNote"))
        );
    }

    // ADMIN — reject
    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Complaint> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(
                complaintService.rejectComplaint(id, body.get("adminNote"))
        );
    }
}