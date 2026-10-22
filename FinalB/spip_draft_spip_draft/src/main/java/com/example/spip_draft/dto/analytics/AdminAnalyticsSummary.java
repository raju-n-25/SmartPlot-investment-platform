package com.example.spip_draft.dto.analytics;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminAnalyticsSummary {

    private long totalProjects;
    private long totalPlots;
    private long totalBuilders;
    private long totalInvestors;

    private BigDecimal platformInventoryValue;
    private BigDecimal platformRevenue;
    private BigDecimal averagePricePerSqft;

    private long highRiskProjects;
    private long mediumRiskProjects;
    private long lowRiskProjects;

    private BigDecimal platformDemandScore;
}