package com.example.spip_draft.mapper;

import com.example.spip_draft.dto.project.ProjectRequest;
import com.example.spip_draft.dto.project.ProjectResponse;
import com.example.spip_draft.entity.Project;

import java.util.List;

public class ProjectMapper {

    public static Project toEntity(ProjectRequest request) {
        Project project = new Project();

        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setLocation(request.getLocation());
        project.setCity(request.getCity());
        project.setState(request.getState());

        project.setTotalPlots(request.getTotalPlots());
        project.setAvailablePlots(request.getAvailablePlots());

        return project;
    }

    public static ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();

        response.setId(project.getId());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        response.setTenantId(project.getTenantId());

        if (project.getBuilder() != null) {
            response.setBuilderId(project.getBuilder().getId());
            response.setBuilderCompanyName(project.getBuilder().getCompanyName());
        }

        response.setProjectName(project.getProjectName());
        response.setDescription(project.getDescription());
        response.setLocation(project.getLocation());
        response.setCity(project.getCity());
        response.setState(project.getState());

        response.setTotalPlots(project.getTotalPlots());
        response.setAvailablePlots(project.getAvailablePlots());

        response.setStatus(
                project.getStatus() != null
                        ? project.getStatus().name()
                        : null
        );

        response.setAmenities(
                project.getAmenities() != null
                        ? project.getAmenities().stream()
                            .map(a -> a.getAmenityType() != null
                                    ? a.getAmenityType().name()
                                    : null)
                            .toList()
                        : List.of()
        );

        return response;
    }
}