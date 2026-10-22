package com.example.spip_draft.dto.wallet;

import com.example.spip_draft.enums.WalletTransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletTransactionResponse {

    private Long transactionId;

    private Long investorId;
    private Long userId;
    private String investorName;
    private String investorEmail;

    private BigDecimal amount;
    private WalletTransactionType transactionType;
    private String description;
    private Long referenceId;

    private LocalDateTime createdAt;
}