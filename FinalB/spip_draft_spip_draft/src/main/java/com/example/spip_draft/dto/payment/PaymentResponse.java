package com.example.spip_draft.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private Long agreementId;

    private BigDecimal amount;
    private BigDecimal tokenPaid;
    private BigDecimal remainingAmount;
    private BigDecimal totalPrice;

    private String status;
    private String bookingStatus;
    private String transactionId;

    private boolean escrowReleased;
    private boolean refunded;

    private String escrowStatus;
    private LocalDateTime escrowHeldAt;
    private LocalDateTime holdUntil;
    private LocalDateTime releasedAt;
    private String releaseReference;

    private Integer riskScore;
    private String riskLevel;
    private String reviewReason;

    private String agreementStatus;
    private String agreementNumber;
    private String agreementFileUrl;
    private boolean agreementCompleted;
    private boolean investorSigned;
    private boolean builderSigned;

    private boolean disputeOpen;
    private boolean payoutBlocked;
    private boolean canRefund;
    private boolean canOpenDispute;
    private boolean canReleaseEscrow;
    private String releaseBlockedReason;

    private LocalDateTime expiryTime;
    private Long secondsRemaining;
    
    private LocalDateTime frozenAt;
    private LocalDateTime freezeDeadlineAt;
    private String freezeActionRequired;
    private LocalDateTime freezeResolvedAt;

    private boolean unfreezeRequested;
    private LocalDateTime unfreezeRequestedAt;
    private String unfreezeRequestNote;
}
