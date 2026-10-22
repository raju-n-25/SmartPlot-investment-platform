package com.example.spip_draft.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvestorDashboardResponse {
    private long blockedPlots;
    private long purchasedPlots;
    private BigDecimal totalInvestment;

    private double roiPercentage;
    private BigDecimal projectedValue3Years;
    private BigDecimal projectedValue5Years;

    private long totalMeetings;
    private long approvedMeetings;
    private long requestedMeetings;
    private long rejectedMeetings;
    private long cancelledMeetings;
    private long completedMeetings;
}