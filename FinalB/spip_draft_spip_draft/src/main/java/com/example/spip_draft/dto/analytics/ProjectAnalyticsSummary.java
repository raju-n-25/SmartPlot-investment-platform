package com.example.spip_draft.dto.analytics;

import com.example.spip_draft.enums.InvestmentRecommendation;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProjectAnalyticsSummary {

    private Long projectId;
    private String projectName;

    private long totalPlots;
    private long availablePlots;
    private long soldPlots;
    private long blockedPlots;

    private BigDecimal totalProjectValue;
    private BigDecimal averagePricePerSqft;
    private BigDecimal demandScore;
    private BigDecimal popularityScore;
    private BigDecimal infrastructureScore;

    private Integer legalRiskScore;
    private String legalRiskCategory;
    private InvestmentRecommendation recommendation;

    private BigDecimal projectedGrowth3Years;
    private BigDecimal projectedGrowth5Years;
}