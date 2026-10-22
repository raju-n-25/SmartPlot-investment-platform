package com.example.spip_draft.dto.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDocumentResponse {

    private Long id;
    private Long userId;
    private String role;

    private String displayName;
    private String email;

    private String documentType;
    private String fileUrl;
    private String originalFileName;

    private String status;
    private String submittedAt;
    private String reviewedAt;

    private String rejectionReason;
    private String resubmissionReason;

    private Integer versionNumber;
    private boolean current;
    private String profilePhotoUrl;

    private Boolean investorApproved;
}