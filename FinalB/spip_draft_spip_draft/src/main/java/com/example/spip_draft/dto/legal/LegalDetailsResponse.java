package com.example.spip_draft.dto.legal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LegalDetailsResponse {

    private Long id;
    private Long plotId;

    private String kathaType;
    private String kathaTypeLabel;

    private boolean reraApproved;
    private boolean dcConverted;
    private int riskScore;

    private boolean safe;
    private boolean highRisk;
}