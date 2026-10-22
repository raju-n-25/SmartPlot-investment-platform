package com.example.spip_draft.entity;

import com.example.spip_draft.enums.KycStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "builders")
@Getter
@Setter
public class Builder extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_registration", nullable = false, unique = true, length = 100)
    private String companyRegistration;

    @Column(name = "pan_number", nullable = false, unique = true, length = 20)
    private String panNumber;

    @Column(name = "gst_number", nullable = false, unique = true, length = 30)
    private String gstNumber;

    @Column(name = "kyc_document_url")
    private String kycDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_submitted_at")
    private LocalDateTime kycSubmittedAt;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_rejection_reason")
    private String kycRejectionReason;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;
}