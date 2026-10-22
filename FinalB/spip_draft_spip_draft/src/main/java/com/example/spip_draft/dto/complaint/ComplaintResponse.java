package com.example.spip_draft.dto.complaint;

import com.example.spip_draft.enums.ComplaintRaisedByRole;
import com.example.spip_draft.enums.ComplaintStatus;
import com.example.spip_draft.enums.ComplaintType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ComplaintResponse {

    private Long id;
    private Long tenantId;

    private ComplaintType complaintType;
    private ComplaintRaisedByRole raisedByRole;
    private ComplaintStatus status;

    private String reason;
    private String description;

    private String adminNote;
    private String resolutionNote;
    private String rejectionReason;

    private Boolean escrowRelated;
    private Boolean paymentRelated;
    private Boolean agreementRelated;

    private Long raisedByUserId;
    private String raisedByUserName;
    private String raisedByUserEmail;

    private Long assignedAdminId;
    private String assignedAdminName;

    private Long resolvedByAdminId;
    private String resolvedByAdminName;

    private Long bookingId;
    private Long paymentId;
    private Long agreementId;
    private Long projectId;
    private Long plotId;

    private String projectName;
    private String plotNumber;
    private String builderName;
    private String investorName;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime rejectedAt;
}