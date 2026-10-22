package com.example.spip_draft.dto.emi;

import com.example.spip_draft.enums.EmiPlanStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmiPlanResponse {

    private Long emiPlanId;
    private Long bookingId;
    private Long investorId;
    private Long tenantId;

    private BigDecimal totalAmount;
    private BigDecimal tokenPaid;
    private BigDecimal downPayment;
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal totalInterest;
    private BigDecimal totalPayable;
    private Integer paidInstallments;
    private Integer totalInstallments;
    private BigDecimal remainingPrincipal;

    private Integer graceDays;
    private BigDecimal lateFeePerDay;
    private BigDecimal maxLateFee;

    private LocalDate startDate;
    private LocalDate firstDueDate;
    private LocalDate nextDueDate;

    private EmiPlanStatus status;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}