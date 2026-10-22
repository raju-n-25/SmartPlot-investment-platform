package com.example.spip_draft.service;

import com.example.spip_draft.dto.project.ProjectCreateResponse;

import com.example.spip_draft.dto.project.ProjectRequest;
import com.example.spip_draft.dto.project.ProjectResponse;
import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.entity.Project;
import com.example.spip_draft.entity.ProjectAmenity;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.enums.KycStatus;
import com.example.spip_draft.enums.PlotApprovalStatus;
import com.example.spip_draft.enums.ProjectStatus;
import com.example.spip_draft.mapper.ProjectMapper;
import com.example.spip_draft.multitenant.TenantContext;
import com.example.spip_draft.repository.BuilderRepository;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.repository.UserRepository;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final BuilderRepository builderRepository;
    private final UserRepository userRepository;
    private final PlotRepository plotRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService; 
    

    public ProjectCreateResponse createProject(ProjectRequest request) {

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Unauthorized access");
        }

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));

        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setLocation(request.getLocation());
        project.setCity(request.getCity());
        project.setState(request.getState());
        project.setTotalPlots(request.getTotalPlots());
        project.setAvailablePlots(request.getAvailablePlots());

        project.setBuilder(builder);
        project.setTenantId(builder.getTenantId());
        project.setStatus(ProjectStatus.PENDING);

        Project saved = projectRepository.save(project);
        auditLogService.log(
                AuditAction.PROJECT_CREATED,
                "PROJECT",
                saved.getId(),
                "Builder created project: " + saved.getProjectName(),
                null,
                saved.getStatus() != null ? saved.getStatus().name() : null
        );

        ProjectCreateResponse response = new ProjectCreateResponse();
        response.setProject(ProjectMapper.toResponse(saved));
        response.setKycStatus(
                builder.getKycStatus() != null ? builder.getKycStatus().name() : null
        );

        if (builder.getKycStatus() == KycStatus.APPROVED) {
            response.setMessage("Project created successfully.");
        } else {
            response.setMessage("Project created. Complete KYC for faster approval.");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects() {
        Long tenantId = getTenantId();
        Builder builder = getCurrentBuilder();

        validateBuilderTenant(builder, tenantId);

        List<Project> projects = projectRepository.findByBuilderId(builder.getId());

        return projects.stream()
                .filter(project -> tenantId.equals(project.getTenantId()))
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId) {
        return mapToResponse(getOwnedProject(projectId));
    }

    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Project project = getOwnedProject(projectId);

        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setLocation(request.getLocation());
        project.setCity(request.getCity());
        project.setState(request.getState());
        project.setTotalPlots(request.getTotalPlots());
        project.setAvailablePlots(request.getAvailablePlots());

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjectsForAdmin() {
        return projectRepository.findAll().stream()
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getPendingProjectsForAdmin() {
        return projectRepository.findAll().stream()
                .filter(project -> project.getStatus() == ProjectStatus.PENDING)
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByBuilder(Long builderId) {
        return projectRepository.findByBuilderId(builderId).stream()
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse approveProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String oldStatus = project.getStatus() != null ? project.getStatus().name() : null;

        project.setStatus(ProjectStatus.APPROVED);

        List<Plot> plots = plotRepository.findByProjectId(id);

        for (Plot plot : plots) {
            if (plot.getBookingStatus() != null && plot.getBookingStatus().isActive()) {
                continue;
            }

            plot.setStatus(PlotApprovalStatus.APPROVED);
            plot.setAvailable(true);
        }

        plotRepository.saveAll(plots);

        Project saved = projectRepository.save(project);

        auditLogService.log(
                AuditAction.PROJECT_APPROVED,
                "PROJECT",
                saved.getId(),
                "Admin approved project and made eligible plots available",
                oldStatus,
                saved.getStatus() != null ? saved.getStatus().name() : null
        );

        return mapToResponse(saved);
    }

    public ProjectResponse rejectProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String oldStatus = project.getStatus() != null ? project.getStatus().name() : null;

        project.setStatus(ProjectStatus.REJECTED);

        List<Plot> plots = plotRepository.findByProjectId(id);

        for (Plot plot : plots) {
            if (plot.getBookingStatus() != null && plot.getBookingStatus().isActive()) {
                continue;
            }

            plot.setStatus(PlotApprovalStatus.REJECTED);
            plot.setAvailable(false);
        }

        plotRepository.saveAll(plots);

        Project saved = projectRepository.save(project);

        auditLogService.log(
                AuditAction.PROJECT_REJECTED,
                "PROJECT",
                saved.getId(),
                "Admin rejected project and disabled plots",
                oldStatus,
                saved.getStatus() != null ? saved.getStatus().name() : null
        );

        return mapToResponse(saved);
    }
    
    public void deleteProject(Long projectId) {
        Project project = getOwnedProject(projectId);

        long bookingCount = bookingRepository.countByPlot_Project_Id(projectId);

        if (bookingCount > 0) {
            throw new RuntimeException("Cannot delete project because bookings exist under this project");
        }

        List<Plot> plots = plotRepository.findByProjectId(projectId);

        for (Plot plot : plots) {
            plot.setAvailable(false);
            plot.setStatus(PlotApprovalStatus.DELETED);
            plot.setBookingStatus(null);
            plot.setBlockedAt(null);
        }

        plotRepository.saveAll(plots);

        String oldStatus = project.getStatus() != null ? project.getStatus().name() : null;

        project.setStatus(ProjectStatus.DELETED);
        projectRepository.save(project);

        auditLogService.log(
                AuditAction.PROJECT_DELETED,
                "PROJECT",
                project.getId(),
                "Builder soft-deleted project: " + project.getProjectName(),
                oldStatus,
                ProjectStatus.DELETED.name()
        );
    }
    
    public Project publishProject(Long projectId) {
        Project project = getOwnedProject(projectId);

        if (project.getStatus() != ProjectStatus.APPROVED) {
            throw new RuntimeException("Only approved projects can be published");
        }

        return project;
    }

    private Project getOwnedProject(Long projectId) {
        Long tenantId = getTenantId();
        Builder builder = getCurrentBuilder();

        validateBuilderTenant(builder, tenantId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getBuilder() == null || !project.getBuilder().getId().equals(builder.getId())) {
            throw new RuntimeException("You are not allowed to access this project");
        }

        if (!tenantId.equals(project.getTenantId())) {
            throw new RuntimeException("You are not allowed to access this tenant's project");
        }

        return project;
    }

    private Builder getCurrentBuilder() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User is not authenticated");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return builderRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Builder profile not found"));
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context is missing");
        }
        return tenantId;
    }

    private void validateBuilderTenant(Builder builder, Long tenantId) {
        if (builder.getTenantId() == null || !builder.getTenantId().equals(tenantId)) {
            throw new RuntimeException("You are not allowed to access this tenant's data");
        }
    }

    private ProjectResponse mapToResponse(Project project) {
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

        if (project.getAmenities() != null && !project.getAmenities().isEmpty()) {
            response.setAmenities(
                    project.getAmenities().stream()
                            .map(ProjectAmenity::getAmenityType)
                            .map(Enum::name)
                            .collect(Collectors.toList())
            );
        } else {
            response.setAmenities(Collections.emptyList());
        }

        response.setStatus(project.getStatus() != null ? project.getStatus().name() : null);

        return response;
    }
}