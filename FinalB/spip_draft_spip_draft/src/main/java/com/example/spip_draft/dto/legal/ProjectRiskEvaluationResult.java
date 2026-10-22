package com.example.spip_draft.dto.legal;

import com.example.spip_draft.enums.ReviewRecommendation;
import com.example.spip_draft.enums.RiskCategory;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectRiskEvaluationResult {

    private Integer riskScore;
    private RiskCategory riskCategory;
    private String legalSummary;
    private ReviewRecommendation reviewRecommendation;
    private List<String> warnings;
}