package com.example.spip_draft.dto.investor;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvestorProfileResponse {

    private Long id;
    private Long userId;
    private Long tenantId;

    private String fullName;
    private String email;
    private String panCard;

    private BigDecimal walletBalance;
    private Integer riskScore;
    private boolean approved;

    private String profilePhotoUrl;
}