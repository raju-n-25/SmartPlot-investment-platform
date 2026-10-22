package com.example.spip_draft.service;

import com.example.spip_draft.dto.legal.PlotRiskEvaluationResult;

import com.example.spip_draft.dto.marketplace.PlotCreateRequest;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.entity.Project;
import com.example.spip_draft.entity.ProjectLegalDetails;
import com.example.spip_draft.enums.PlotApprovalStatus;
import com.example.spip_draft.multitenant.TenantContext;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.repository.ProjectLegalDetailsRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlotService {

    private final PlotRepository plotRepository;
    private final ProjectRepository projectRepository;
    private final ProjectLegalDetailsRepository projectLegalDetailsRepository;
    private final RiskEvaluationService riskEvaluationService;
    private final AuditLogService auditLogService;

    public Plot addPlot(Long projectId, PlotCreateRequest request) {
        Long tenantId = getTenant();
        Project project = getProject(projectId);

        validateTenantOwnership(project, tenantId);
        validateDuplicatePlotNumber(projectId, request.getPlotNumber());

        Plot plot = mapToPlot(request, project);
        evaluateRiskIfPossible(plot, project);

        Plot saved = plotRepository.save(plot);

        auditLogService.log(
                AuditAction.PLOT_CREATED,
                "PLOT",
                saved.getId(),
                "Builder created plot " + saved.getPlotNumber() + " under project " + project.getId(),
                null,
                saved.getStatus() != null ? saved.getStatus().name() : null
        );

        return saved;
    }

    public List<Plot> addPlotsBulk(Long projectId, List<PlotCreateRequest> requests) {
        Long tenantId = getTenant();
        Project project = getProject(projectId);

        validateTenantOwnership(project, tenantId);

        List<Plot> plots = new ArrayList<>();

        for (PlotCreateRequest request : requests) {
            validateDuplicatePlotNumber(projectId, request.getPlotNumber());

            Plot plot = mapToPlot(request, project);
            evaluateRiskIfPossible(plot, project);
            plots.add(plot);
        }

        List<Plot> savedPlots = plotRepository.saveAll(plots);

        auditLogService.log(
                AuditAction.PLOT_BULK_CREATED,
                "PROJECT",
                project.getId(),
                "Builder bulk-created " + savedPlots.size() + " plots under project " + project.getId()
        );

        return savedPlots;
    }

    @Transactional(readOnly = true)
    public List<Plot> getPlotsByProject(Long projectId) {
        Long tenantId = getTenant();
        Project project = getProject(projectId);

        validateTenantOwnership(project, tenantId);
        return plotRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Plot getPlot(Long projectId, Long plotId) {
        Long tenantId = getTenant();
        Project project = getProject(projectId);

        validateTenantOwnership(project, tenantId);

        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new RuntimeException("Plot not found"));

        if (!plot.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Plot does not belong to this project");
        }

        return plot;
    }

    public void deletePlot(Long projectId, Long plotId) {
        Long tenantId = getTenant();
        Project project = getProject(projectId);

        validateTenantOwnership(project, tenantId);

        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new RuntimeException("Plot not found"));

        if (!plot.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Plot does not belong to this project");
        }

        if (plot.getBookingStatus() != null && plot.getBookingStatus().isActive()) {
            throw new RuntimeException("Cannot delete plot because it has active booking or meeting hold");
        }

        if (plot.isSold()) {
            throw new RuntimeException("Cannot delete sold plot");
        }

        String oldStatus = plot.getStatus() != null ? plot.getStatus().name() : null;

        plot.setAvailable(false);
        plot.setStatus(PlotApprovalStatus.DELETED);
        plot.setBookingStatus(null);
        plot.setBlockedAt(null);

        plotRepository.save(plot);

        auditLogService.log(
                AuditAction.PLOT_DELETED,
                "PLOT",
                plot.getId(),
                "Builder soft-deleted plot " + plot.getPlotNumber(),
                oldStatus,
                PlotApprovalStatus.DELETED.name()
        );
    }

    public void approvePlot(Long plotId) {
        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new RuntimeException("Plot not found"));

        plot.setStatus(PlotApprovalStatus.APPROVED);

        boolean projectApproved =
                plot.getProject() != null
                        && plot.getProject().getStatus() != null
                        && plot.getProject().getStatus().isApproved();

        plot.setAvailable(projectApproved);

        plotRepository.save(plot);
    }

    public void rejectPlot(Long plotId) {
        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new RuntimeException("Plot not found"));

        plot.setStatus(PlotApprovalStatus.REJECTED);
        plot.setAvailable(false);
        plotRepository.save(plot);
    }

    private Plot mapToPlot(PlotCreateRequest request, Project project) {
        Plot plot = new Plot();

        plot.setProject(project);
        plot.setTenantId(project.getTenantId());

        plot.setPlotNumber(cleanPlotNumber(request.getPlotNumber()));
        plot.setArea(request.getArea());
        plot.setPrice(java.math.BigDecimal.valueOf(request.getPrice()));
        plot.setFacing(request.getFacing());
        plot.setKhataType(request.getKhataType());
        plot.setKhataRemarks(request.getKhataRemarks());

        if (request.getPlotDescription() != null && !request.getPlotDescription().trim().isEmpty()) {
            plot.setPlotDescription(request.getPlotDescription().trim());
        }

        if (request.getPlotDimensions() != null && !request.getPlotDimensions().trim().isEmpty()) {
            plot.setPlotDimensions(request.getPlotDimensions().trim());
        }

        if (request.getRoadWidth() != null) {
            plot.setRoadWidth(request.getRoadWidth());
        }

        if (request.getMapLink() != null && !request.getMapLink().trim().isEmpty()) {
            plot.setMapLink(request.getMapLink().trim());
        }

        plot.setCornerPlot(Boolean.TRUE.equals(request.getCornerPlot()));

        if (request.getLegalInfo() != null && !request.getLegalInfo().trim().isEmpty()) {
            plot.setLegalInfo(request.getLegalInfo().trim());
        }

        if (request.getRiskScore() != null) {
            plot.setRiskScore(request.getRiskScore());
        }

        boolean projectApproved =
                project.getStatus() != null && project.getStatus().isApproved();

        plot.setStatus(projectApproved ? PlotApprovalStatus.APPROVED : PlotApprovalStatus.PENDING);
        plot.setAvailable(projectApproved);
        plot.setBookingStatus(null);
        plot.setBlockedAt(null);

        return plot;
    }

    private void evaluateRiskIfPossible(Plot plot, Project project) {
        ProjectLegalDetails legal = projectLegalDetailsRepository.findByProject_Id(project.getId()).orElse(null);
        if (legal == null) {
            return;
        }

        try {
            Integer projectRiskScore = legal.getProjectRiskScore();
            PlotRiskEvaluationResult result =
                    riskEvaluationService.evaluatePlotRisk(projectRiskScore, plot.getKhataType());

            if (result != null) {
                if (result.getPlotRiskCategory() != null) {
                    plot.setPlotRiskCategory(result.getPlotRiskCategory());
                }
                if (result.getPlotRiskScore() != null) {
                    plot.setPlotRiskScore(result.getPlotRiskScore());
                    plot.setRiskScore(result.getPlotRiskScore().doubleValue());
                }
            }
        } catch (Exception ignored) {
            // keep plot creation resilient even if risk evaluation fails
        }
    }

    private void validateDuplicatePlotNumber(Long projectId, String plotNumber) {
        String cleaned = cleanPlotNumber(plotNumber);

        boolean exists = plotRepository.existsByProjectIdAndPlotNumber(projectId, cleaned);
        if (exists) {
            throw new RuntimeException("Plot number already exists in this project: " + cleaned);
        }
    }

    private String cleanPlotNumber(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new RuntimeException("Plot number is required");
        }
        return value.trim().toUpperCase();
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    private Long getTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context is missing");
        }
        return tenantId;
    }

    private void validateTenantOwnership(Project project, Long tenantId) {
        if (project.getTenantId() == null || !project.getTenantId().equals(tenantId)) {
            throw new RuntimeException("You do not have access to this project");
        }
    }

    public Plot updatePlot(Long projectId, Long plotId, PlotCreateRequest request) {
        Long tenantId = getTenant();
        Project project = getProject(projectId);

        validateTenantOwnership(project, tenantId);

        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new RuntimeException("Plot not found"));

        if (!plot.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Plot does not belong to this project");
        }

        String oldPlotNumber = plot.getPlotNumber();

        String incomingPlotNumber = cleanPlotNumber(request.getPlotNumber());
        if (!plot.getPlotNumber().equalsIgnoreCase(incomingPlotNumber)) {
            validateDuplicatePlotNumber(projectId, incomingPlotNumber);
        }

        plot.setPlotNumber(incomingPlotNumber);
        plot.setArea(request.getArea());
        plot.setPrice(java.math.BigDecimal.valueOf(request.getPrice()));
        plot.setFacing(request.getFacing());
        plot.setKhataType(request.getKhataType());
        plot.setKhataRemarks(request.getKhataRemarks());
        plot.setCornerPlot(Boolean.TRUE.equals(request.getCornerPlot()));

        if (request.getAvailable() != null) {
            boolean projectApproved =
                    project.getStatus() != null && project.getStatus().isApproved();

            if (!projectApproved && Boolean.TRUE.equals(request.getAvailable())) {
                throw new RuntimeException("Cannot make plot available before project approval");
            }

            plot.setAvailable(request.getAvailable());
        }

        if (request.getPlotDescription() != null && !request.getPlotDescription().trim().isEmpty()) {
            plot.setPlotDescription(request.getPlotDescription().trim());
        } else {
            plot.setPlotDescription(null);
        }

        if (request.getPlotDimensions() != null && !request.getPlotDimensions().trim().isEmpty()) {
            plot.setPlotDimensions(request.getPlotDimensions().trim());
        } else {
            plot.setPlotDimensions(null);
        }

        if (request.getRoadWidth() != null) {
            plot.setRoadWidth(request.getRoadWidth());
        } else {
            plot.setRoadWidth(null);
        }

        if (request.getMapLink() != null && !request.getMapLink().trim().isEmpty()) {
            plot.setMapLink(request.getMapLink().trim());
        } else {
            plot.setMapLink(null);
        }

        if (request.getLegalInfo() != null && !request.getLegalInfo().trim().isEmpty()) {
            plot.setLegalInfo(request.getLegalInfo().trim());
        } else {
            plot.setLegalInfo(null);
        }

        if (request.getRiskScore() != null) {
            plot.setRiskScore(request.getRiskScore());
        } else {
            plot.setRiskScore(null);
        }

        evaluateRiskIfPossible(plot, project);

        Plot saved = plotRepository.save(plot);

        auditLogService.log(
                AuditAction.PLOT_UPDATED,
                "PLOT",
                saved.getId(),
                "Builder updated plot " + oldPlotNumber,
                oldPlotNumber,
                saved.getPlotNumber()
        );

        return saved;
    }
}