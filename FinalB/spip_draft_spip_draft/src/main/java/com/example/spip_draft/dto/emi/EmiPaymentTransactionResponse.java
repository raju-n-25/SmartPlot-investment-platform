package com.example.spip_draft.dto.emi;

import com.example.spip_draft.enums.EmiPaymentMode;
import com.example.spip_draft.enums.EmiPaymentTransactionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EmiPaymentTransactionResponse {

    private Long transactionId;
    private Long emiPlanId;
    private Long emiInstallmentId;
    private Long investorId;
    private Long tenantId;

    private BigDecimal amount;
    private BigDecimal walletAmount;
    private BigDecimal gatewayAmount;

    private EmiPaymentMode paymentMode;
    private EmiPaymentTransactionStatus status;

    private String gatewayType;
    private String gatewayReference;
    private String remarks;
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}