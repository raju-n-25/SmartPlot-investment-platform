package com.example.spip_draft.controller;

import com.example.spip_draft.dto.legal.ProjectLegalDetailsRequest;
import com.example.spip_draft.dto.legal.ProjectLegalDetailsResponse;
import com.example.spip_draft.service.ProjectLegalDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builder/projects")
@RequiredArgsConstructor
public class BuilderProjectLegalController {

    private final ProjectLegalDetailsService projectLegalDetailsService;

    @PostMapping("/{projectId}/legal-details")
    public ResponseEntity<ProjectLegalDetailsResponse> saveOrUpdateLegalDetails(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectLegalDetailsRequest request
    ) {
        return ResponseEntity.ok(projectLegalDetailsService.saveOrUpdate(projectId, request));
    }

    @GetMapping("/{projectId}/legal-details")
    public ResponseEntity<ProjectLegalDetailsResponse> getLegalDetails(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectLegalDetailsService.getByProjectId(projectId));
    }
}