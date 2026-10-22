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
@RequestMapping("/api/builder/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BuilderDocumentController {

    private final UserDocumentService userDocumentService;

    @PostMapping
    public ResponseEntity<UserDocumentResponse> uploadBuilderDocument(
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "originalFileName", required = false) String originalFileName,
            @RequestParam(value = "resubmissionReason", required = false) String resubmissionReason
    ) {
        return ResponseEntity.ok(
                userDocumentService.uploadBuilderDocument(
                        documentType,
                        file,
                        originalFileName,
                        resubmissionReason
                )
        );
    }

    @GetMapping("/current")
    public ResponseEntity<List<UserDocumentResponse>> getCurrentBuilderDocuments() {
        return ResponseEntity.ok(userDocumentService.getCurrentBuilderDocuments());
    }

    @GetMapping("/history")
    public ResponseEntity<List<UserDocumentResponse>> getAllBuilderDocuments() {
        return ResponseEntity.ok(userDocumentService.getAllBuilderDocuments());
    }
}