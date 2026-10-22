package com.example.spip_draft.service;

import com.example.spip_draft.dto.project.ProjectAmenityRequest;
import com.example.spip_draft.dto.project.ProjectAmenityResponse;

import java.util.List;

public interface ProjectAmenityService {

    ProjectAmenityResponse addAmenity(Long projectId, ProjectAmenityRequest request);

    List<ProjectAmenityResponse> addAmenities(Long projectId, List<ProjectAmenityRequest> requests);

    List<ProjectAmenityResponse> getAmenities(Long projectId);

    ProjectAmenityResponse updateAmenity(Long projectId, Long amenityId, ProjectAmenityRequest request);

    void deleteAmenity(Long projectId, Long amenityId);
}