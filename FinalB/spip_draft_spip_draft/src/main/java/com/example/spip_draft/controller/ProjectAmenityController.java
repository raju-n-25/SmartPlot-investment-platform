package com.example.spip_draft.controller;

import com.example.spip_draft.dto.project.ProjectAmenityRequest;
import com.example.spip_draft.dto.project.ProjectAmenityResponse;
import com.example.spip_draft.service.ProjectAmenityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builder/projects/{projectId}/amenities")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ProjectAmenityController {

    private final ProjectAmenityService projectAmenityService;

    @PostMapping
    public ResponseEntity<ProjectAmenityResponse> addAmenity(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectAmenityRequest request
    ) {
        return ResponseEntity.ok(projectAmenityService.addAmenity(projectId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ProjectAmenityResponse>> addAmenities(
            @PathVariable Long projectId,
            @Valid @RequestBody List<ProjectAmenityRequest> requests
    ) {
        return ResponseEntity.ok(projectAmenityService.addAmenities(projectId, requests));
    }

    @GetMapping
    public ResponseEntity<List<ProjectAmenityResponse>> getAmenities(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectAmenityService.getAmenities(projectId));
    }
    
    @PutMapping("/{amenityId}")
    public ResponseEntity<ProjectAmenityResponse> updateAmenity(
            @PathVariable Long projectId,
            @PathVariable Long amenityId,
            @Valid @RequestBody ProjectAmenityRequest request
    ) {
        return ResponseEntity.ok(projectAmenityService.updateAmenity(projectId, amenityId, request));
    }

    @DeleteMapping("/{amenityId}")
    public ResponseEntity<Void> deleteAmenity(
            @PathVariable Long projectId,
            @PathVariable Long amenityId
    ) {
        projectAmenityService.deleteAmenity(projectId, amenityId);
        return ResponseEntity.noContent().build();
    }
}