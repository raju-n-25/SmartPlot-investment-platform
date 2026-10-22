package com.example.spip_draft.dto.emi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayEmiInstallmentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
    private BigDecimal amount;

    private boolean useWallet;

    private String gatewayType;

    @DecimalMin(value = "0.00", inclusive = true, message = "Wallet amount cannot be negative")
    private BigDecimal walletAmount = BigDecimal.ZERO;

    private String remarks;
}