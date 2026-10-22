package com.example.spip_draft.dto.emi;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ForeclosureResponse {

    private Long emiPlanId;
    private BigDecimal remainingPrincipal;
    private BigDecimal unpaidPenalty;
    private BigDecimal foreclosureFee;
    private BigDecimal totalForeclosureAmount;
    private String message;
}