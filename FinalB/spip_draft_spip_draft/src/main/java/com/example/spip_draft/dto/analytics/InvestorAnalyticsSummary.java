package com.example.spip_draft.dto.analytics;

import com.example.spip_draft.enums.InvestmentRecommendation;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvestorAnalyticsSummary {

    private double roiPercentage;

    private BigDecimal totalInvestment;
    private BigDecimal currentPortfolioValue;

    private BigDecimal projectedValue3Years;
    private BigDecimal projectedValue5Years;

    private BigDecimal projectedProfit3Years;
    private BigDecimal projectedProfit5Years;

    private BigDecimal averagePricePerSqft;

    private InvestmentRecommendation recommendation;
    private BigDecimal demandScore;
    private BigDecimal infrastructureScore;
}