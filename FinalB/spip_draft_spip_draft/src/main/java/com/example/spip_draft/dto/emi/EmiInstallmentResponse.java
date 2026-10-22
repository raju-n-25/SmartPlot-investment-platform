package com.example.spip_draft.dto.emi;

import com.example.spip_draft.enums.InstallmentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmiInstallmentResponse {

    private Long installmentId;
    private Long emiPlanId;
    private Integer installmentNumber;

    private LocalDate dueDate;
    private BigDecimal openingPrincipal;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal emiAmount;
    private BigDecimal penaltyAmount;

    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal penaltyPaid;

    private BigDecimal paidAmount;
    private BigDecimal remainingDue;

    private LocalDate paidDate;
    private LocalDateTime lastPaymentAt;
    private InstallmentStatus status;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}