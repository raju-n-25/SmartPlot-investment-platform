package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.project.ProjectAmenityRequest;
import com.example.spip_draft.dto.project.ProjectAmenityResponse;
import com.example.spip_draft.entity.Project;
import com.example.spip_draft.entity.ProjectAmenity;
import com.example.spip_draft.multitenant.TenantContext;
import com.example.spip_draft.repository.ProjectAmenityRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.service.ProjectAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectAmenityServiceImpl implements ProjectAmenityService {

    private final ProjectRepository projectRepository;
    private final ProjectAmenityRepository projectAmenityRepository;

    @Override
    public ProjectAmenityResponse addAmenity(Long projectId, ProjectAmenityRequest request) {

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Unauthorized");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access");
        }

        ProjectAmenity amenity = new ProjectAmenity();
        amenity.setProject(project);
        amenity.setAmenityType(request.getAmenityType());
        amenity.setDescription(request.getDescription());

        ProjectAmenity saved = projectAmenityRepository.save(amenity);
        return mapToResponse(saved);
    }

    @Override
    public List<ProjectAmenityResponse> addAmenities(Long projectId, List<ProjectAmenityRequest> requests) {
        return requests.stream()
                .map(request -> addAmenity(projectId, request))
                .toList();
    }

    @Override
    public List<ProjectAmenityResponse> getAmenities(Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Project not found");
        }

        return projectAmenityRepository.findByProject_Id(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProjectAmenityResponse mapToResponse(ProjectAmenity amenity) {
        return ProjectAmenityResponse.builder()
                .id(amenity.getId())
                .projectId(amenity.getProject().getId())
                .amenityType(amenity.getAmenityType() != null ? amenity.getAmenityType().name() : null)
                .description(amenity.getDescription())
                .build();
    }
    @Override
    public ProjectAmenityResponse updateAmenity(
            Long projectId,
            Long amenityId,
            ProjectAmenityRequest request
    ) {
        ProjectAmenity amenity = projectAmenityRepository.findById(amenityId)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        if (!amenity.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Amenity does not belong to this project");
        }

        amenity.setAmenityType(request.getAmenityType());
        amenity.setDescription(request.getDescription());

        ProjectAmenity updated = projectAmenityRepository.save(amenity);

        return mapToResponse(updated);
    }
    
    @Override
    public void deleteAmenity(Long projectId, Long amenityId) {

        ProjectAmenity amenity = projectAmenityRepository.findById(amenityId)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        if (!amenity.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Amenity does not belong to this project");
        }

        projectAmenityRepository.delete(amenity);
    }
}