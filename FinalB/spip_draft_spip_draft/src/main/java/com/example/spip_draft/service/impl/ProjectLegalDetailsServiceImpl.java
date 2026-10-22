package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.legal.ProjectLegalDetailsRequest;
import com.example.spip_draft.dto.legal.ProjectLegalDetailsResponse;
import com.example.spip_draft.dto.legal.ProjectLegalReviewResponse;
import com.example.spip_draft.dto.legal.ProjectRiskEvaluationResult;
import com.example.spip_draft.entity.Project;
import com.example.spip_draft.entity.ProjectLegalDetails;
import com.example.spip_draft.repository.ProjectLegalDetailsRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.service.ProjectLegalDetailsService;
import com.example.spip_draft.service.RiskEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectLegalDetailsServiceImpl implements ProjectLegalDetailsService {

    private final ProjectRepository projectRepository;
    private final ProjectLegalDetailsRepository projectLegalDetailsRepository;
    private final RiskEvaluationService riskEvaluationService;

    @Override
    public ProjectLegalDetailsResponse saveOrUpdate(Long projectId, ProjectLegalDetailsRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectLegalDetails legalDetails = projectLegalDetailsRepository.findByProject_Id(projectId)
                .orElseGet(() -> {
                    ProjectLegalDetails newLegalDetails = new ProjectLegalDetails();
                    newLegalDetails.setProject(project);
                    return newLegalDetails;
                });

        legalDetails.setReraRegistrationNumber(request.getReraRegistrationNumber());
        legalDetails.setReraStatus(request.getReraStatus());
        legalDetails.setDcConversionStatus(request.getDcConversionStatus());
        legalDetails.setDocumentStatus(request.getDocumentStatus());
        legalDetails.setLegalNotes(request.getLegalNotes());

        ProjectRiskEvaluationResult evaluation = riskEvaluationService.evaluateProjectRisk(legalDetails);

        legalDetails.setProjectRiskScore(evaluation.getRiskScore());
        legalDetails.setProjectRiskCategory(evaluation.getRiskCategory());
        legalDetails.setLegalSummary(evaluation.getLegalSummary());
        legalDetails.setReviewRecommendation(evaluation.getReviewRecommendation());

        ProjectLegalDetails saved = projectLegalDetailsRepository.save(legalDetails);

        return ProjectLegalDetailsResponse.builder()
                .id(saved.getId())
                .projectId(saved.getProject().getId())
                .reraRegistrationNumber(saved.getReraRegistrationNumber())
                .reraStatus(saved.getReraStatus() != null ? saved.getReraStatus().name() : null)
                .dcConversionStatus(saved.getDcConversionStatus() != null ? saved.getDcConversionStatus().name() : null)
                .documentStatus(saved.getDocumentStatus() != null ? saved.getDocumentStatus().name() : null)
                .legalNotes(saved.getLegalNotes())
                .projectRiskScore(saved.getProjectRiskScore())
                .projectRiskCategory(saved.getProjectRiskCategory() != null ? saved.getProjectRiskCategory().name() : null)
                .legalSummary(saved.getLegalSummary())
                .reviewRecommendation(saved.getReviewRecommendation() != null ? saved.getReviewRecommendation().name() : null)
                .build();
    }

    @Override
    public ProjectLegalDetailsResponse getByProjectId(Long projectId) {

        ProjectLegalDetails legalDetails = projectLegalDetailsRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new RuntimeException("Project legal details not found"));

        return ProjectLegalDetailsResponse.builder()
                .id(legalDetails.getId())
                .projectId(legalDetails.getProject().getId())
                .reraRegistrationNumber(legalDetails.getReraRegistrationNumber())
                .reraStatus(legalDetails.getReraStatus() != null ? legalDetails.getReraStatus().name() : null)
                .dcConversionStatus(legalDetails.getDcConversionStatus() != null ? legalDetails.getDcConversionStatus().name() : null)
                .documentStatus(legalDetails.getDocumentStatus() != null ? legalDetails.getDocumentStatus().name() : null)
                .legalNotes(legalDetails.getLegalNotes())
                .projectRiskScore(legalDetails.getProjectRiskScore())
                .projectRiskCategory(legalDetails.getProjectRiskCategory() != null ? legalDetails.getProjectRiskCategory().name() : null)
                .legalSummary(legalDetails.getLegalSummary())
                .reviewRecommendation(legalDetails.getReviewRecommendation() != null ? legalDetails.getReviewRecommendation().name() : null)
                .build();
    }

    @Override
    public ProjectLegalReviewResponse reviewProject(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<String> warnings = new ArrayList<>();

        ProjectLegalDetails legalDetails = projectLegalDetailsRepository.findByProject_Id(projectId)
                .orElse(null);

        if (legalDetails == null) {
            warnings.add("Project legal details are not submitted yet");

            return ProjectLegalReviewResponse.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .projectStatus(project.getStatus() != null ? project.getStatus().name() : null)
                    .reraRegistrationNumber(null)
                    .reraStatus("NOT_SUBMITTED")
                    .dcConversionStatus("NOT_APPLICABLE")
                    .documentStatus("NOT_UPLOADED")
                    .projectRiskScore(100)
                    .projectRiskCategory("HIGH")
                    .legalSummary("Project legal details are not submitted yet. Overall project risk is HIGH.")
                    .reviewRecommendation("REJECT_OR_HOLD")
                    .warnings(warnings)
                    .build();
        }

        ProjectRiskEvaluationResult evaluation = riskEvaluationService.evaluateProjectRisk(legalDetails);

        return ProjectLegalReviewResponse.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .projectStatus(project.getStatus() != null ? project.getStatus().name() : null)
                .reraRegistrationNumber(legalDetails.getReraRegistrationNumber())
                .reraStatus(legalDetails.getReraStatus() != null ? legalDetails.getReraStatus().name() : null)
                .dcConversionStatus(legalDetails.getDcConversionStatus() != null ? legalDetails.getDcConversionStatus().name() : null)
                .documentStatus(legalDetails.getDocumentStatus() != null ? legalDetails.getDocumentStatus().name() : null)
                .projectRiskScore(evaluation.getRiskScore())
                .projectRiskCategory(evaluation.getRiskCategory().name())
                .legalSummary(evaluation.getLegalSummary())
                .reviewRecommendation(evaluation.getReviewRecommendation().name())
                .warnings(evaluation.getWarnings())
                .build();
    }
}