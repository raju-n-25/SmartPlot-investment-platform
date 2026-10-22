package com.example.spip_draft.entity;

import com.example.spip_draft.enums.AgreementStatus;
import com.example.spip_draft.enums.EscrowStatus;
import com.example.spip_draft.enums.PaymentStatus;
import com.example.spip_draft.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payment_booking", columnList = "booking_id"),
                @Index(name = "idx_payment_tenant", columnList = "tenant_id"),
                @Index(name = "idx_payment_status", columnList = "payment_status"),
                @Index(name = "idx_payment_escrow_status", columnList = "escrow_status"),
                @Index(name = "idx_payment_hold_until", columnList = "hold_until")
        }
)
@Getter
@Setter
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "transaction_id", unique = true, length = 150)
    private String transactionId;

    @Column(name = "is_escrow_released", nullable = false)
    private boolean escrowReleased = false;

    @Column(name = "is_refunded", nullable = false)
    private boolean refunded = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", length = 50)
    private EscrowStatus escrowStatus;

    @Column(name = "escrow_held_at")
    private LocalDateTime escrowHeldAt;

    @Column(name = "hold_until")
    private LocalDateTime holdUntil;

    @Column(name = "ready_for_release_at")
    private LocalDateTime readyForReleaseAt;

    @Column(name = "release_scheduled_at")
    private LocalDateTime releaseScheduledAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reference", length = 150)
    private String releaseReference;

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_status", length = 50)
    private AgreementStatus agreementStatus = AgreementStatus.NOT_REQUIRED;

    @Column(name = "dispute_open", nullable = false)
    private boolean disputeOpen = false;

    @Column(name = "payout_blocked", nullable = false)
    private boolean payoutBlocked = false;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Column(name = "freeze_deadline_at")
    private LocalDateTime freezeDeadlineAt;

    @Column(name = "freeze_action_required", length = 500)
    private String freezeActionRequired;

    @Column(name = "freeze_resolved_at")
    private LocalDateTime freezeResolvedAt;
    
    @Column(name = "unfreeze_requested", nullable = false)
    private boolean unfreezeRequested = false;

    @Column(name = "unfreeze_requested_at")
    private LocalDateTime unfreezeRequestedAt;

    @Column(name = "unfreeze_request_note", length = 500)
    private String unfreezeRequestNote;

    public boolean isTokenPaid() {
        return this.paymentStatus == PaymentStatus.TOKEN_PAID;
    }

    public boolean isCompleted() {
        return this.paymentStatus == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.paymentStatus == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return this.refunded;
    }

    public boolean isExpired() {
        return expiryTime != null && expiryTime.isBefore(LocalDateTime.now());
    }

    public boolean isAgreementSatisfied() {
        return agreementStatus == AgreementStatus.SIGNED;
    }

    public void markTokenPaid(BigDecimal amount) {
        this.amount = amount;
        this.paymentStatus = PaymentStatus.TOKEN_PAID;
        this.expiryTime = LocalDateTime.now().plusMinutes(10);
    }

    public void markCompleted(String txnId, BigDecimal totalAmount) {
        this.transactionId = txnId;
        this.amount = totalAmount;
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.escrowReleased = false;
        this.escrowStatus = EscrowStatus.HELD;
        this.escrowHeldAt = LocalDateTime.now();
        this.holdUntil = LocalDateTime.now().plusHours(24);
        this.readyForReleaseAt = null;
        this.releaseScheduledAt = null;
        this.releasedAt = null;
        this.releaseReference = null;
        this.expiryTime = null;

        if (this.agreementStatus == null || this.agreementStatus == AgreementStatus.NOT_REQUIRED) {
            this.agreementStatus = AgreementStatus.PENDING_GENERATION;
        }
    }

    public void markUnderReview(String reason) {
        this.escrowStatus = EscrowStatus.UNDER_REVIEW;
        this.reviewReason = reason;
    }

    public void markReadyForRelease() {
        this.escrowStatus = EscrowStatus.READY_FOR_RELEASE;
        this.readyForReleaseAt = LocalDateTime.now();
        this.reviewReason = null;
    }

    public void markReleaseScheduled() {
        this.escrowStatus = EscrowStatus.RELEASE_SCHEDULED;
        this.releaseScheduledAt = LocalDateTime.now();
    }

    public void markEscrowReleased() {
        this.escrowReleased = true;
        this.escrowStatus = EscrowStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        this.releaseReference = "PAYOUT_" + UUID.randomUUID();
        this.reviewReason = null;
    }

    public void markReleaseFailed(String reason) {
        this.escrowStatus = EscrowStatus.RELEASE_FAILED;
        this.reviewReason = reason;
    }

    public void markFrozen(String reason, String actionRequired, int deadlineDays) {
        this.escrowStatus = EscrowStatus.FROZEN;
        this.payoutBlocked = true;
        this.reviewReason = reason;
        this.freezeActionRequired = actionRequired;
        this.frozenAt = LocalDateTime.now();
        this.freezeDeadlineAt = LocalDateTime.now().plusDays(deadlineDays);
        this.freezeResolvedAt = null;
    }
    
    public void requestUnfreezeReview(String note) {
        this.unfreezeRequested = true;
        this.unfreezeRequestedAt = LocalDateTime.now();
        this.unfreezeRequestNote =
                note == null || note.isBlank()
                        ? "Investor completed required action and requested escrow review."
                        : note;
    }

    public void unfreezeToReview(String note) {
        this.payoutBlocked = false;

        if (this.escrowStatus == EscrowStatus.FROZEN) {
            this.escrowStatus = EscrowStatus.UNDER_REVIEW;
        }

        this.reviewReason = note;
        this.freezeResolvedAt = LocalDateTime.now();

        this.unfreezeRequested = false;
        this.unfreezeRequestedAt = null;
        this.unfreezeRequestNote = null;
    }
    public void openDispute(String reason) {
        this.disputeOpen = true;
        this.escrowStatus = EscrowStatus.UNDER_REVIEW;
        this.reviewReason = reason;
    }

    public void closeDispute() {
        this.disputeOpen = false;
    }

    public void markRefundPending() {
        this.escrowStatus = EscrowStatus.REFUND_PENDING;
    }

    public void markFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void markRefunded() {
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.refunded = true;
        this.refundedAt = LocalDateTime.now();
        this.escrowStatus = EscrowStatus.REFUNDED;
        this.escrowReleased = false;
    }
    
}