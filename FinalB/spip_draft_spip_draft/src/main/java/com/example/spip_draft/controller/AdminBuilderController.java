package com.example.spip_draft.controller;

import com.example.spip_draft.dto.builder.BuilderKycActionRequest;
import com.example.spip_draft.dto.builder.BuilderProfileResponse;
import com.example.spip_draft.dto.document.UserDocumentResponse;
import com.example.spip_draft.service.BuilderService;
import com.example.spip_draft.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/builders")
@RequiredArgsConstructor
public class AdminBuilderController {

    private final BuilderService builderService;
    private final UserDocumentService userDocumentService;

    // 👑 Admin → View all builders
    @GetMapping
    public ResponseEntity<List<BuilderProfileResponse>> getAllBuilders() {
        return ResponseEntity.ok(builderService.getAllBuilders());
    }

    // 👑 Admin → Get builder by ID
    @GetMapping("/{id}")
    public ResponseEntity<BuilderProfileResponse> getBuilder(@PathVariable Long id) {
        return ResponseEntity.ok(builderService.getBuilderProfileById(id));
    }

    // 👑 Admin → View pending KYC builders
    @GetMapping("/pending-kyc")
    public ResponseEntity<List<BuilderProfileResponse>> getPendingKycBuilders() {
        return ResponseEntity.ok(builderService.getPendingKycBuilders());
    }

    // 👑 Admin → Get builder current documents
    @GetMapping("/{id}/documents/current")
    public ResponseEntity<List<UserDocumentResponse>> getBuilderCurrentDocuments(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userDocumentService.getBuilderCurrentDocumentsForAdmin(id));
    }

    // 👑 Admin → Get builder document history
    @GetMapping("/{id}/documents/history")
    public ResponseEntity<List<UserDocumentResponse>> getBuilderDocumentHistory(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userDocumentService.getBuilderDocumentHistoryForAdmin(id));
    }

    // 👑 Admin → Approve Builder KYC
    @PutMapping("/{id}/approve")
    public ResponseEntity<String> approveBuilder(@PathVariable Long id) {
        builderService.approveBuilder(id);
        return ResponseEntity.ok("Builder KYC approved successfully");
    }

    // 👑 Admin → Reject Builder KYC
    @PutMapping("/{id}/reject")
    public ResponseEntity<String> rejectBuilder(
            @PathVariable Long id,
            @RequestBody(required = false) BuilderKycActionRequest request
    ) {
        String rejectionReason = request != null ? request.getRejectionReason() : null;
        builderService.rejectBuilder(id, rejectionReason);
        return ResponseEntity.ok("Builder KYC rejected successfully");
    }

    // 👑 Admin → Delete Builder
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBuilder(@PathVariable Long id) {
        builderService.deleteBuilder(id);
        return ResponseEntity.ok("Builder deleted successfully");
    }
}