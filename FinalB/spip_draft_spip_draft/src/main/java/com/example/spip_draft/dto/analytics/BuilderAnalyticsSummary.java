package com.example.spip_draft.dto.analytics;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BuilderAnalyticsSummary {

    private long totalProjects;
    private long totalPlots;
    private long availablePlots;
    private long soldPlots;
    private long blockedPlots;

    private BigDecimal totalInventoryValue;
    private BigDecimal totalRevenue;
    private BigDecimal averagePricePerSqft;

    private long totalBookings;
    private long last30DaysBookings;

    private BigDecimal demandScore;
    private BigDecimal projectPopularityScore;
}