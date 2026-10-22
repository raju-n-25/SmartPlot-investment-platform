package com.example.spip_draft.service;

import com.example.spip_draft.dto.analytics.AdminAnalyticsSummary;
import com.example.spip_draft.dto.analytics.BuilderAnalyticsSummary;
import com.example.spip_draft.dto.analytics.HeatmapPointDto;
import com.example.spip_draft.dto.analytics.InvestorAnalyticsSummary;
import com.example.spip_draft.dto.analytics.ProjectAnalyticsSummary;

import java.math.BigDecimal;
import java.util.List;

public interface AnalyticsService {

    double calculateROI(double investment, double returns);

    BigDecimal projectFutureValue(BigDecimal currentValue, BigDecimal annualAppreciationRate, int years);

    InvestorAnalyticsSummary getInvestorAnalyticsSummary(Long investorId, BigDecimal appreciationRate);

    BuilderAnalyticsSummary getBuilderAnalyticsSummary(Long builderId);

    AdminAnalyticsSummary getAdminAnalyticsSummary();

    ProjectAnalyticsSummary getProjectAnalyticsSummary(Long projectId, BigDecimal appreciationRate);

    List<HeatmapPointDto> getProjectHeatmap(Long projectId, BigDecimal appreciationRate);
}