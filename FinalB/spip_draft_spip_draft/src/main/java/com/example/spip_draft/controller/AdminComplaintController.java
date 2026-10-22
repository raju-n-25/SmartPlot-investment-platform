package com.example.spip_draft.controller;

import com.example.spip_draft.dto.complaint.ComplaintActionRequest;
import com.example.spip_draft.dto.complaint.ComplaintResponse;
import com.example.spip_draft.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaintsForAdmin());
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<ComplaintResponse> markUnderReview(
            @PathVariable Long id,
            @RequestBody(required = false) ComplaintActionRequest request
    ) {
        return ResponseEntity.ok(complaintService.markUnderReview(id, request));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<ComplaintResponse> resolveComplaint(
            @PathVariable Long id,
            @RequestBody(required = false) ComplaintActionRequest request
    ) {
        return ResponseEntity.ok(complaintService.resolveComplaint(id, request));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ComplaintResponse> rejectComplaint(
            @PathVariable Long id,
            @RequestBody(required = false) ComplaintActionRequest request
    ) {
        return ResponseEntity.ok(complaintService.rejectComplaint(id, request));
    }
}