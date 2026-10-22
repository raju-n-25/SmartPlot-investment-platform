package com.example.spip_draft.dto.dashboard;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

import java.math.BigDecimal;

@Getter
@Setter
public class BuilderDashboardResponse {
    private long totalProjects;
    private long totalPlots;
    private long availablePlots;
    private long blockedPlots;
    private long soldPlots;

    private BigDecimal totalRevenue;
    private BigDecimal tokenRevenue;
    private BigDecimal completedRevenue;

    private long totalBookings;
    private long last30DaysBookings;

    private long lowRiskProjects;
    private long mediumRiskProjects;
    private long highRiskProjects;

    private long pendingMeetingRequests;
    private long approvedMeetings;
    private long completedMeetings;
    private long rejectedMeetings;
    private List<RecentActivityDTO> recentActivities;
}