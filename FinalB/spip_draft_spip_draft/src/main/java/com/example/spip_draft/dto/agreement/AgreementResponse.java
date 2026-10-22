package com.example.spip_draft.dto.agreement;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgreementResponse {

    private Long id;
    private Long bookingId;
    private Long paymentId;
    private Long tenantId;
    private Long investorId;
    private Long builderId;

    private String agreementNumber;
    private String agreementType;
    private String fileUrl;
    private String reviewFileUrl;
    private String executedFileUrl;
    private String status;
    private String currentStage;
    private String nextAction;
    private String nextActionMessage;
    private String remarks;

    private String investorName;
    private String builderName;
    private String projectName;
    private String plotNumber;

    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private LocalDateTime investorViewedAt;
    private LocalDateTime builderViewedAt;
    private LocalDateTime investorAcceptedAt;
    private LocalDateTime builderAcceptedAt;
    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime investorSignedAt;
    private LocalDateTime builderSignedAt;

    private boolean investorViewed;
    private boolean builderViewed;
    private boolean investorSigned;
    private boolean builderSigned;
    private boolean completed;
    private boolean rejected;
    private boolean expired;
    private boolean executedDocumentAvailable;

    private boolean currentUserInvestor;
    private boolean currentUserBuilder;
    private boolean currentUserAdmin;

    private boolean canSend;
    private boolean canMarkViewed;
    private boolean canInvestorAccept;
    private boolean canBuilderAccept;
    private boolean canInvestorSign;
    private boolean canBuilderSign;
    private boolean canRejectAndRefund;

    private boolean mustOpenBeforeAccept;
    private boolean mustOpenBeforeSign;
}