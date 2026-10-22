package com.example.spip_draft.dto.legal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectLegalReviewResponse {

    private Long projectId;
    private String projectName;
    private String projectStatus;

    private String reraRegistrationNumber;
    private String reraStatus;
    private String dcConversionStatus;
    private String documentStatus;

    private Integer projectRiskScore;
    private String projectRiskCategory;
    private String legalSummary;
    private String reviewRecommendation;

    private List<String> warnings;
}