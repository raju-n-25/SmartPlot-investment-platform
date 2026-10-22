package com.example.spip_draft.entity;

import com.example.spip_draft.enums.ComplaintRaisedByRole;
import com.example.spip_draft.enums.ComplaintStatus;
import com.example.spip_draft.enums.ComplaintType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter
@Setter
public class Complaint extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "complaint_type", nullable = false, length = 60)
    private ComplaintType complaintType = ComplaintType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "raised_by_role", nullable = false, length = 30)
    private ComplaintRaisedByRole raisedByRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "escrow_related", nullable = false)
    private Boolean escrowRelated = false;

    @Column(name = "payment_related", nullable = false)
    private Boolean paymentRelated = false;

    @Column(name = "agreement_related", nullable = false)
    private Boolean agreementRelated = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_user_id", nullable = false)
    private User raisedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_admin_id")
    private User resolvedByAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private BookingAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id")
    private Plot plot;

    @PrePersist
    protected void onCreateComplaint() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = ComplaintStatus.OPEN;
        }

        if (complaintType == null) {
            complaintType = ComplaintType.GENERAL;
        }

        if (escrowRelated == null) {
            escrowRelated = false;
        }

        if (paymentRelated == null) {
            paymentRelated = false;
        }

        if (agreementRelated == null) {
            agreementRelated = false;
        }
    }

    public void markUnderReview(User admin, String note) {
        this.status = ComplaintStatus.UNDER_REVIEW;
        this.assignedAdmin = admin;
        this.adminNote = note;
        this.reviewedAt = LocalDateTime.now();
    }

    public void markResolved(User admin, String note) {
        this.status = ComplaintStatus.RESOLVED;
        this.resolvedByAdmin = admin;
        this.resolutionNote = note;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markRejected(User admin, String reason) {
        this.status = ComplaintStatus.REJECTED;
        this.resolvedByAdmin = admin;
        this.rejectionReason = reason;
        this.rejectedAt = LocalDateTime.now();
    }
}