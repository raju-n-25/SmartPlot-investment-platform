package com.example.spip_draft.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminDashboardResponse {
    private long totalBuilders;
    private long pendingKycApprovals;
    private long totalProjects;
    private BigDecimal totalRevenue;
    private long highRiskProjects;
    private long bookingConflictLogs;
    private BigDecimal escrowHeld;
    private BigDecimal escrowReleased;

    // =========================
    // NEW DOCUMENT SUMMARY
    // =========================
    private long pendingBuilderDocuments;
    private long pendingInvestorDocuments;
    private long approvedBuilderDocuments;
    private long approvedInvestorDocuments;
    private long rejectedBuilderDocuments;
    private long rejectedInvestorDocuments;
    private long approvedBuilders;
    private long rejectedBuilders;
}