package com.example.spip_draft.controller;

import com.example.spip_draft.dto.legal.ProjectDocumentResponse;
import com.example.spip_draft.dto.legal.ProjectLegalDetailsResponse;
import com.example.spip_draft.dto.legal.ProjectLegalReviewResponse;
import com.example.spip_draft.dto.project.ProjectResponse;
import com.example.spip_draft.service.ProjectDocumentService;
import com.example.spip_draft.service.ProjectLegalDetailsService;
import com.example.spip_draft.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminProjectController {

    private final ProjectService projectService;
    private final ProjectLegalDetailsService projectLegalDetailsService;
    private final ProjectDocumentService projectDocumentService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjectsForAdmin());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ProjectResponse>> getPendingProjects() {
        return ResponseEntity.ok(projectService.getPendingProjectsForAdmin());
    }

    @GetMapping("/builder/{builderId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByBuilder(
            @PathVariable Long builderId
    ) {
        return ResponseEntity.ok(projectService.getProjectsByBuilder(builderId));
    }

    @GetMapping("/{projectId}/legal-details")
    public ResponseEntity<ProjectLegalDetailsResponse> getProjectLegalDetails(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectLegalDetailsService.getByProjectId(projectId));
    }

    @GetMapping("/{projectId}/documents")
    public ResponseEntity<List<ProjectDocumentResponse>> getProjectDocuments(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectDocumentService.getProjectDocuments(projectId));
    }

    @GetMapping("/{projectId}/review")
    public ResponseEntity<ProjectLegalReviewResponse> reviewProject(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectLegalDetailsService.reviewProject(projectId));
    }

    @PutMapping("/{projectId}/approve")
    public ResponseEntity<ProjectResponse> approveProject(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectService.approveProject(projectId));
    }

    @PutMapping("/{projectId}/reject")
    public ResponseEntity<ProjectResponse> rejectProject(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectService.rejectProject(projectId));
    }
}