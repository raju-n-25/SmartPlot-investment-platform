package com.example.spip_draft.dto.legal;

import lombok.Data;

@Data
public class LegalDetailsRequest {

    private Long plotId;

    private String kathaType;

    // ✅ IMPORTANT FIX
    private Boolean reraApproved;

    private Boolean dcConverted;

    // ✅ FIX (was int)
    private Integer riskScore;
}