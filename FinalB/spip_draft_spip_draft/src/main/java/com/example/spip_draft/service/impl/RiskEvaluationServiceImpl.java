package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.legal.PlotRiskEvaluationResult;
import com.example.spip_draft.dto.legal.ProjectRiskEvaluationResult;
import com.example.spip_draft.entity.ProjectLegalDetails;
import com.example.spip_draft.enums.DcConversionStatus;
import com.example.spip_draft.enums.DocumentStatus;
import com.example.spip_draft.enums.KathaType;
import com.example.spip_draft.enums.ReraStatus;
import com.example.spip_draft.enums.ReviewRecommendation;
import com.example.spip_draft.enums.RiskCategory;
import com.example.spip_draft.service.RiskEvaluationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEvaluationServiceImpl implements RiskEvaluationService {

    @Override
    public ProjectRiskEvaluationResult evaluateProjectRisk(ProjectLegalDetails legalDetails) {

        int score = 0;
        List<String> warnings = new ArrayList<>();

        if (legalDetails.getReraStatus() == null || legalDetails.getReraStatus() == ReraStatus.NOT_SUBMITTED) {
            score += 40;
            warnings.add("RERA is not submitted");
        } else if (legalDetails.getReraStatus() == ReraStatus.SUBMITTED) {
            score += 20;
            warnings.add("RERA is submitted but not yet verified");
        } else if (legalDetails.getReraStatus() == ReraStatus.EXEMPT) {
            score += 10;
            warnings.add("Project is marked as RERA exempt");
        }

        if (legalDetails.getReraStatus() == ReraStatus.VERIFIED || legalDetails.getReraStatus() == ReraStatus.SUBMITTED) {
            if (legalDetails.getReraRegistrationNumber() == null || legalDetails.getReraRegistrationNumber().isBlank()) {
                score += 10;
                warnings.add("RERA registration number is missing");
            }
        }

        if (legalDetails.getDcConversionStatus() == null || legalDetails.getDcConversionStatus() == DcConversionStatus.PENDING) {
            score += 10;
            warnings.add("DC conversion is pending");
        } else if (legalDetails.getDcConversionStatus() == DcConversionStatus.NOT_CONVERTED) {
            score += 15;
            warnings.add("Land is not converted");
        }

        if (legalDetails.getDocumentStatus() == null || legalDetails.getDocumentStatus() == DocumentStatus.NOT_UPLOADED) {
            score += 20;
            warnings.add("Required legal documents are not uploaded");
        } else if (legalDetails.getDocumentStatus() == DocumentStatus.PARTIAL) {
            score += 10;
            warnings.add("Legal documents are only partially available");
        }

        if (score > 100) {
            score = 100;
        }

        RiskCategory category;
        if (score >= 55) {
            category = RiskCategory.HIGH;
        } else if (score >= 25) {
            category = RiskCategory.MEDIUM;
        } else {
            category = RiskCategory.LOW;
        }

        ReviewRecommendation recommendation;
        if (category == RiskCategory.HIGH) {
            recommendation = ReviewRecommendation.REJECT_OR_HOLD;
        } else if (category == RiskCategory.MEDIUM) {
            recommendation = ReviewRecommendation.MANUAL_REVIEW_REQUIRED;
        } else {
            recommendation = ReviewRecommendation.APPROVE;
        }

        String legalSummary = buildSummary(legalDetails, category);

        return ProjectRiskEvaluationResult.builder()
                .riskScore(score)
                .riskCategory(category)
                .legalSummary(legalSummary)
                .reviewRecommendation(recommendation)
                .warnings(warnings)
                .build();
    }

    @Override
    public PlotRiskEvaluationResult evaluatePlotRisk(Integer projectRiskScore, KathaType khataType) {

        int score = projectRiskScore != null ? projectRiskScore : 0;

        if (khataType == null) {
            score += 10;
        } else if (khataType == KathaType.B) {
            score += 20;
        } else if (khataType == KathaType.E) {
            score += 30;
        }

        if (score > 100) {
            score = 100;
        }

        RiskCategory category;
        if (score >= 55) {
            category = RiskCategory.HIGH;
        } else if (score >= 25) {
            category = RiskCategory.MEDIUM;
        } else {
            category = RiskCategory.LOW;
        }

        return PlotRiskEvaluationResult.builder()
                .plotRiskScore(score)
                .plotRiskCategory(category)
                .build();
    }

    private String buildSummary(ProjectLegalDetails legalDetails, RiskCategory category) {

        String reraStatus = legalDetails.getReraStatus() != null
                ? legalDetails.getReraStatus().name()
                : "NOT_SUBMITTED";

        String dcStatus = legalDetails.getDcConversionStatus() != null
                ? legalDetails.getDcConversionStatus().name()
                : "NOT_APPLICABLE";

        String documentStatus = legalDetails.getDocumentStatus() != null
                ? legalDetails.getDocumentStatus().name()
                : "NOT_UPLOADED";

        return "RERA status is " + reraStatus +
                ", DC conversion status is " + dcStatus +
                ", document status is " + documentStatus +
                ". Overall project risk is " + category.name() + ".";
    }
}