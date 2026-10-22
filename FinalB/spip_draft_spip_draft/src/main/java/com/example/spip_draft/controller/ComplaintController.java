package com.example.spip_draft.controller;

import com.example.spip_draft.dto.complaint.ComplaintRequest;
import com.example.spip_draft.dto.complaint.ComplaintResponse;
import com.example.spip_draft.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComplaintController {

    private final ComplaintService complaintService;
    
    @PostMapping("/general")
    public ResponseEntity<ComplaintResponse> createGeneralComplaint(
            @RequestBody ComplaintRequest request
    ) {
        return ResponseEntity.ok(
                complaintService.createGeneralComplaint(request)
        );
    }

    @PostMapping("/booking/{bookingId}")
    public ResponseEntity<ComplaintResponse> createBookingComplaint(
            @PathVariable Long bookingId,
            @RequestBody ComplaintRequest request
    ) {
        return ResponseEntity.ok(
                complaintService.createBookingComplaint(bookingId, request)
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints() {
        return ResponseEntity.ok(complaintService.getMyComplaints());
    }
}