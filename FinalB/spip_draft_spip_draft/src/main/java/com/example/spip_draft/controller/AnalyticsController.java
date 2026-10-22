package com.example.spip_draft.controller;

import com.example.spip_draft.dto.analytics.AdminAnalyticsSummary;
import com.example.spip_draft.dto.analytics.BuilderAnalyticsSummary;
import com.example.spip_draft.dto.analytics.HeatmapPointDto;
import com.example.spip_draft.dto.analytics.InvestorAnalyticsSummary;
import com.example.spip_draft.dto.analytics.ProjectAnalyticsSummary;
import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.repository.BuilderRepository;
import com.example.spip_draft.service.AnalyticsService;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final BuilderRepository builderRepository;

    @GetMapping("/roi")
    public ResponseEntity<Map<String, Double>> calculateROI(
            @RequestParam double investment,
            @RequestParam double returns) {

        double roi = analyticsService.calculateROI(investment, returns);
        return ResponseEntity.ok(Map.of("roi", roi));
    }

    @GetMapping("/investor/summary")
    public ResponseEntity<InvestorAnalyticsSummary> getInvestorSummary(
            @RequestParam(required = false) BigDecimal appreciationRate
    ) {
        Long investorUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getInvestorAnalyticsSummary(investorUserId, appreciationRate));
    }

    @GetMapping("/builder/summary")
    public ResponseEntity<BuilderAnalyticsSummary> getBuilderSummary() {
        Long tenantId = SecurityUtil.getCurrentTenantId();

        Builder builder = builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Builder not found"));

        return ResponseEntity.ok(analyticsService.getBuilderAnalyticsSummary(builder.getId()));
    }

    @GetMapping("/admin/summary")
    public ResponseEntity<AdminAnalyticsSummary> getAdminSummary() {
        return ResponseEntity.ok(analyticsService.getAdminAnalyticsSummary());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ProjectAnalyticsSummary> getProjectSummary(
            @PathVariable Long projectId,
            @RequestParam(required = false) BigDecimal appreciationRate
    ) {
        return ResponseEntity.ok(analyticsService.getProjectAnalyticsSummary(projectId, appreciationRate));
    }

    @GetMapping("/project/{projectId}/heatmap")
    public ResponseEntity<List<HeatmapPointDto>> getProjectHeatmap(
            @PathVariable Long projectId,
            @RequestParam(required = false) BigDecimal appreciationRate
    ) {
        return ResponseEntity.ok(analyticsService.getProjectHeatmap(projectId, appreciationRate));
    }
}