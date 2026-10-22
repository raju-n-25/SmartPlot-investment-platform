package com.example.spip_draft.controller;

import com.example.spip_draft.dto.dashboard.AdminDashboardResponse;
import com.example.spip_draft.dto.dashboard.BuilderDashboardResponse;
import com.example.spip_draft.dto.dashboard.InvestorDashboardResponse;
import com.example.spip_draft.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.spip_draft.dto.dashboard.AdminConflictLogResponse;
import com.example.spip_draft.dto.dashboard.EscrowDetailResponse;
import java.util.List;
import com.example.spip_draft.dto.dashboard.MeetingWidgetResponse;
import com.example.spip_draft.dto.dashboard.PaymentWidgetResponse;


import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/api/builder/dashboard")
    public ResponseEntity<BuilderDashboardResponse> getBuilderDashboard() {
        return ResponseEntity.ok(dashboardService.getBuilderDashboard());
    }

    @GetMapping("/api/investors/dashboard")
    public ResponseEntity<InvestorDashboardResponse> getInvestorDashboard(
            @RequestParam(required = false) BigDecimal appreciationRate
    ) {
        return ResponseEntity.ok(dashboardService.getInvestorDashboard(appreciationRate));
    }
    @GetMapping("/api/admin/conflicts")
    public ResponseEntity<List<AdminConflictLogResponse>> getRecentConflictLogs() {
        return ResponseEntity.ok(dashboardService.getRecentConflictLogs());
    }

    @GetMapping("/api/admin/escrow-details")
    public ResponseEntity<List<EscrowDetailResponse>> getEscrowDetails() {
        return ResponseEntity.ok(dashboardService.getEscrowDetails());
    }
    @GetMapping("/api/builder/dashboard/recent-meetings")
    public ResponseEntity<List<MeetingWidgetResponse>> getRecentBuilderMeetings() {
        return ResponseEntity.ok(dashboardService.getRecentBuilderMeetings());
    }

    @GetMapping("/api/investors/dashboard/recent-meetings")
    public ResponseEntity<List<MeetingWidgetResponse>> getRecentInvestorMeetings() {
        return ResponseEntity.ok(dashboardService.getRecentInvestorMeetings());
    }
    @GetMapping("/api/admin/dashboard/recent-payments")
    public ResponseEntity<List<PaymentWidgetResponse>> getRecentAdminPayments() {
        return ResponseEntity.ok(dashboardService.getRecentAdminPayments());
    }

    @GetMapping("/api/builder/dashboard/recent-payments")
    public ResponseEntity<List<PaymentWidgetResponse>> getRecentBuilderPayments() {
        return ResponseEntity.ok(dashboardService.getRecentBuilderPayments());
    }
}