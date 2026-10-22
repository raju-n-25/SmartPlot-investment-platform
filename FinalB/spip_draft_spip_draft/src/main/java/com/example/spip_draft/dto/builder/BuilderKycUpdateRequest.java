package com.example.spip_draft.dto.builder;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuilderKycUpdateRequest {

    @NotBlank(message = "KYC document URL is required")
    private String kycDocumentUrl;
}