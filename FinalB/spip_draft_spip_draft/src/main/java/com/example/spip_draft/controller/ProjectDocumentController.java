package com.example.spip_draft.controller;

import com.example.spip_draft.dto.legal.ProjectDocumentResponse;
import com.example.spip_draft.enums.DocumentType;
import com.example.spip_draft.service.ProjectDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/builder/projects")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ProjectDocumentController {

    private final ProjectDocumentService projectDocumentService;

    // =========================
    // 📤 Upload document
    // =========================
    @PostMapping("/{projectId}/documents")
    public ResponseEntity<ProjectDocumentResponse> uploadDocument(
            @PathVariable Long projectId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                projectDocumentService.uploadDocument(projectId, documentType, file)
        );
    }

    // =========================
    // 📄 Get all documents
    // =========================
    @GetMapping("/{projectId}/documents")
    public ResponseEntity<List<ProjectDocumentResponse>> getDocuments(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(
                projectDocumentService.getProjectDocuments(projectId)
        );
    }
}