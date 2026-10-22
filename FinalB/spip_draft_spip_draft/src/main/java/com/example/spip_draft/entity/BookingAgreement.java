package com.example.spip_draft.entity;

import com.example.spip_draft.enums.AgreementStatus;
import com.example.spip_draft.enums.AgreementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_agreements",
        indexes = {
                @Index(name = "idx_agreement_booking", columnList = "booking_id"),
                @Index(name = "idx_agreement_payment", columnList = "payment_id"),
                @Index(name = "idx_agreement_status", columnList = "status"),
                @Index(name = "idx_agreement_tenant", columnList = "tenant_id"),
                @Index(name = "idx_agreement_type", columnList = "agreement_type")
        })
@Getter
@Setter
public class BookingAgreement extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "builder_id", nullable = false)
    private Long builderId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "agreement_number", nullable = false, unique = true, length = 100)
    private String agreementNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false, length = 50)
    private AgreementType agreementType = AgreementType.FULL_PAYMENT;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "review_file_url", length = 1000)
    private String reviewFileUrl;

    @Column(name = "executed_file_url", length = 1000)
    private String executedFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AgreementStatus status = AgreementStatus.PENDING_GENERATION;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "investor_accepted_at")
    private LocalDateTime investorAcceptedAt;

    @Column(name = "builder_accepted_at")
    private LocalDateTime builderAcceptedAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "investor_signed")
    private Boolean investorSigned = false;

    @Column(name = "builder_signed")
    private Boolean builderSigned = false;

    @Column(name = "investor_signed_at")
    private LocalDateTime investorSignedAt;

    @Column(name = "builder_signed_at")
    private LocalDateTime builderSignedAt;

    @Column(name = "investor_viewed_document")
    private Boolean investorViewedDocument = false;

    @Column(name = "builder_viewed_document")
    private Boolean builderViewedDocument = false;

    @Column(name = "investor_viewed_document_at")
    private LocalDateTime investorViewedDocumentAt;

    @Column(name = "builder_viewed_document_at")
    private LocalDateTime builderViewedDocumentAt;

    @Column(name = "last_action_message", length = 500)
    private String lastActionMessage;

    @Column(name = "last_action_by", length = 100)
    private String lastActionBy;
}