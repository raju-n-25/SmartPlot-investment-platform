package com.example.spip_draft.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {

    private Long id;
    private Long tenantId;

    private Long plotId;
    private String plotNumber;
    private String projectName;

    private Long investorId;
    private String investorName;

    private Long builderId;
    private String builderName;

    private BigDecimal amount;
    private BigDecimal tokenPaid;
    private BigDecimal totalPrice;
    private BigDecimal remainingAmount;

    private String status;
    private LocalDateTime bookingDate;
    private LocalDateTime tokenExpiry;

    private LocalDateTime builderResponseAt;
    private String builderResponseNote;
    private Long builderApprovedBy;

    private String paymentStatus;
    private String escrowStatus;
    private String riskLevel;
    private String agreementStatus;
    private String agreementType;

    private boolean escrowReleased;
    private boolean refunded;
    private boolean disputeOpen;
    private boolean refundScheduled;

    private LocalDateTime escrowHeldAt;
    private LocalDateTime holdUntil;
    private LocalDateTime readyForReleaseAt;
    private LocalDateTime releaseScheduledAt;
    private LocalDateTime releasedAt;
    private String releaseReference;
    private LocalDateTime refundedAt;

    private Long agreementId;
    private String agreementNumber;
    private String agreementFileUrl;
    private String agreementReviewFileUrl;
    private String agreementExecutedFileUrl;
    private boolean investorSigned;
    private boolean builderSigned;
    private boolean investorViewedDocument;
    private boolean builderViewedDocument;
    private boolean executedAgreementAvailable;

    private LocalDateTime agreementGeneratedAt;
    private LocalDateTime agreementSentAt;
    private LocalDateTime investorAcceptedAt;
    private LocalDateTime builderAcceptedAt;
    private LocalDateTime investorSignedAt;
    private LocalDateTime builderSignedAt;
    private LocalDateTime agreementSignedAt;
    private LocalDateTime agreementExpiresAt;
    
    private String reviewReason;
    private String freezeActionRequired;
    private LocalDateTime frozenAt;
    private LocalDateTime freezeDeadlineAt;
    private LocalDateTime freezeResolvedAt;

    private boolean unfreezeRequested;
    private LocalDateTime unfreezeRequestedAt;
    private String unfreezeRequestNote;
    
    
}