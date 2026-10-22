package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.analytics.InvestorAnalyticsSummary;
import com.example.spip_draft.service.AnalyticsService;
import com.example.spip_draft.service.InvestorAnalyticsService;
import org.springframework.context.annotation.Primary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Primary  
@RequiredArgsConstructor
public class InvestorAnalyticsServiceImpl implements InvestorAnalyticsService {

    private final AnalyticsService analyticsService;

    @Override
    public InvestorAnalyticsSummary getInvestorAnalyticsSummary(Long investorId,
                                                                BigDecimal totalInvestment,
                                                                BigDecimal appreciationRate) {
        InvestorAnalyticsSummary summary = analyticsService.getInvestorAnalyticsSummary(investorId, appreciationRate);
        summary.setTotalInvestment(totalInvestment);
        return summary;
    }
}