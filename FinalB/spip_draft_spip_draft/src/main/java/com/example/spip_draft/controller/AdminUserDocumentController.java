package com.example.spip_draft.controller;

import com.example.spip_draft.dto.document.DocumentReviewRequest;
import com.example.spip_draft.dto.document.UserDocumentResponse;
import com.example.spip_draft.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminUserDocumentController {

    private final UserDocumentService userDocumentService;

    @GetMapping("/pending/{role}")
    public ResponseEntity<List<UserDocumentResponse>> getPendingDocumentsByRole(
            @PathVariable String role
    ) {
        return ResponseEntity.ok(userDocumentService.getPendingDocuments(role));
    }
    
    @GetMapping("/current/{role}")
    public ResponseEntity<List<UserDocumentResponse>> getCurrentDocumentsByRole(
            @PathVariable String role
    ) {
        return ResponseEntity.ok(userDocumentService.getCurrentDocumentsByRole(role));
    }

    @PutMapping("/{documentId}/approve")
    public ResponseEntity<UserDocumentResponse> approveDocument(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(userDocumentService.approveDocument(documentId));
    }

    @PutMapping("/{documentId}/reject")
    public ResponseEntity<UserDocumentResponse> rejectDocument(
            @PathVariable Long documentId,
            @RequestBody(required = false) DocumentReviewRequest request
    ) {
        String reason = request != null ? request.getRejectionReason() : null;
        return ResponseEntity.ok(userDocumentService.rejectDocument(documentId, reason));
    }
    @GetMapping("/reviewed/{role}/{status}")
    public ResponseEntity<List<UserDocumentResponse>> getReviewedDocuments(
            @PathVariable String role,
            @PathVariable String status
    ) {
        return ResponseEntity.ok(
                userDocumentService.getDocumentsByRoleAndStatus(role, status)
        );
    }
    
    @PutMapping("/investor/{userId}/approve")
    public ResponseEntity<Void> approveInvestorKyc(
            @PathVariable Long userId
    ) {
        userDocumentService.refreshInvestorApprovalStatus(userId);
        return ResponseEntity.ok().build();
    }
}