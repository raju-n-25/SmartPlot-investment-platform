package com.example.spip_draft.service;

import com.example.spip_draft.dto.analytics.InvestorAnalyticsSummary;

import java.math.BigDecimal;

public interface InvestorAnalyticsService {
    InvestorAnalyticsSummary getInvestorAnalyticsSummary(Long investorId, BigDecimal totalInvestment, BigDecimal appreciationRate);
}