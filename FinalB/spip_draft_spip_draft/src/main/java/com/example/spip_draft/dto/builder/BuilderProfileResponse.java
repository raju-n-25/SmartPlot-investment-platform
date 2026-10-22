package com.example.spip_draft.dto.builder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuilderProfileResponse {

    private Long id;
    private Long userId;
    private Long tenantId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;

    private String companyName;
    private String companyRegistration;
    private String panNumber;
    private String gstNumber;

    private String kycDocumentUrl;
    private String kycStatus;
    private String kycSubmittedAt;
    private String kycVerifiedAt;
    private String kycRejectionReason;

    private Double rating;

    private String profilePhotoUrl;
}