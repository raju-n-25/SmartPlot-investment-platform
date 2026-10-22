package com.example.spip_draft.entity;

import com.example.spip_draft.enums.DcConversionStatus;
import com.example.spip_draft.enums.DocumentStatus;
import com.example.spip_draft.enums.ReraStatus;
import com.example.spip_draft.enums.ReviewRecommendation;
import com.example.spip_draft.enums.RiskCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_legal_details")
@Getter
@Setter
public class ProjectLegalDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(name = "rera_registration_number", length = 100)
    private String reraRegistrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "rera_status", nullable = false)
    private ReraStatus reraStatus = ReraStatus.NOT_SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "dc_conversion_status", nullable = false)
    private DcConversionStatus dcConversionStatus = DcConversionStatus.NOT_APPLICABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false)
    private DocumentStatus documentStatus = DocumentStatus.NOT_UPLOADED;

    @Column(name = "legal_notes", length = 2000)
    private String legalNotes;

    @Column(name = "project_risk_score")
    private Integer projectRiskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_risk_category")
    private RiskCategory projectRiskCategory = RiskCategory.LOW;

    @Column(name = "legal_summary", length = 3000)
    private String legalSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_recommendation")
    private ReviewRecommendation reviewRecommendation = ReviewRecommendation.APPROVE;
}