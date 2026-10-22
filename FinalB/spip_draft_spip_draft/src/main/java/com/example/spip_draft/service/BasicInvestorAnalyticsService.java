package com.example.spip_draft.service;

import com.example.spip_draft.dto.analytics.InvestorAnalyticsSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BasicInvestorAnalyticsService implements InvestorAnalyticsService {

    @Override
    public InvestorAnalyticsSummary getInvestorAnalyticsSummary(
            Long investorId,
            BigDecimal totalInvestment,
            BigDecimal appreciationRate
    ) {
        InvestorAnalyticsSummary summary = new InvestorAnalyticsSummary();

        BigDecimal rate = appreciationRate == null ? new BigDecimal("0.10") : appreciationRate;

        if (totalInvestment == null) {
            totalInvestment = BigDecimal.ZERO;
        }

        BigDecimal projected3 = totalInvestment
                .multiply(BigDecimal.ONE.add(rate))
                .multiply(BigDecimal.ONE.add(rate))
                .multiply(BigDecimal.ONE.add(rate));

        BigDecimal projected5 = totalInvestment
                .multiply(BigDecimal.ONE.add(rate))
                .multiply(BigDecimal.ONE.add(rate))
                .multiply(BigDecimal.ONE.add(rate))
                .multiply(BigDecimal.ONE.add(rate))
                .multiply(BigDecimal.ONE.add(rate));

        summary.setProjectedValue3Years(projected3.setScale(2, RoundingMode.HALF_UP));
        summary.setProjectedValue5Years(projected5.setScale(2, RoundingMode.HALF_UP));

        if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal roi = projected5.subtract(totalInvestment)
                    .divide(totalInvestment, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            summary.setRoiPercentage(roi.doubleValue());
        } else {
            summary.setRoiPercentage(0.0);
        }

        // placeholders for future advanced analytics integration
        summary.setRecommendation(null);
        summary.setDemandScore(null);
        summary.setInfrastructureScore(null);

        return summary;
    }
}