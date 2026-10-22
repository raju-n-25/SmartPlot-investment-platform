package com.example.spip_draft.dto.legal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectLegalDetailsResponse {

    private Long id;
    private Long projectId;

    private String reraRegistrationNumber;
    private String reraStatus;
    private String dcConversionStatus;
    private String documentStatus;

    private String legalNotes;

    private Integer projectRiskScore;
    private String projectRiskCategory;
    private String legalSummary;
    private String reviewRecommendation;
}