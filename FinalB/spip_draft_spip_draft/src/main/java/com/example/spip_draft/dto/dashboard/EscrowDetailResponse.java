package com.example.spip_draft.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EscrowDetailResponse {
    private Long paymentId;
    private Long bookingId;
    private Long plotId;

    private String projectName;
    private String builderName;
    private String builderCompanyName;
    private String investorName;

    private BigDecimal tokenAmount;
    private BigDecimal fullAmount;

    private BigDecimal amount;
    private String paymentStatus;
    private String escrowStatus;
    private Boolean escrowReleased;

    private String releaseMode;      // AUTO / ADMIN / UNKNOWN
    private LocalDateTime releasedAt;
    private String releaseReference;

    private String riskLevel;
    private Integer riskScore;

    private LocalDateTime createdAt;
    
    private LocalDateTime holdUntil;
    private LocalDateTime readyForReleaseAt;
    private LocalDateTime releaseScheduledAt;
    private Boolean canReleaseEscrow;
    private String releaseBlockedReason;
    private String reviewReason;
    
    private LocalDateTime frozenAt;
    private LocalDateTime freezeDeadlineAt;
    private String freezeActionRequired;
    private LocalDateTime freezeResolvedAt;
    private boolean unfreezeRequested;
    private LocalDateTime unfreezeRequestedAt;
    private String unfreezeRequestNote;
}