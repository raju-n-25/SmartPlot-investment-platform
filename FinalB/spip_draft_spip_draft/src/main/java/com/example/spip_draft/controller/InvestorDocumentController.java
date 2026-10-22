package com.example.spip_draft.controller;

import com.example.spip_draft.dto.document.UserDocumentResponse;
import com.example.spip_draft.enums.DocumentType;
import com.example.spip_draft.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/investors/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvestorDocumentController {

    private final UserDocumentService userDocumentService;

    @PostMapping
    public ResponseEntity<UserDocumentResponse> uploadInvestorDocument(
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "originalFileName", required = false) String originalFileName,
            @RequestParam(value = "resubmissionReason", required = false) String resubmissionReason
    ) {
        return ResponseEntity.ok(
                userDocumentService.uploadInvestorDocument(
                        documentType,
                        file,
                        originalFileName,
                        resubmissionReason
                )
        );
    }

    @GetMapping("/current")
    public ResponseEntity<List<UserDocumentResponse>> getCurrentInvestorDocuments() {
        return ResponseEntity.ok(userDocumentService.getCurrentInvestorDocuments());
    }

    @GetMapping("/history")
    public ResponseEntity<List<UserDocumentResponse>> getAllInvestorDocuments() {
        return ResponseEntity.ok(userDocumentService.getAllInvestorDocuments());
    }
}