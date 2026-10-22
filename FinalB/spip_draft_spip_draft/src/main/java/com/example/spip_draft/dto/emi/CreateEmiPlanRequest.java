package com.example.spip_draft.dto.emi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateEmiPlanRequest {

    @NotNull(message = "Down payment is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Down payment cannot be negative")
    private BigDecimal downPayment;

    @NotNull(message = "Annual interest rate is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Interest rate cannot be negative")
    private BigDecimal annualInterestRate;

    @NotNull(message = "Tenure months is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 360, message = "Tenure is too large")
    private Integer tenureMonths;

    @NotNull(message = "First due date is required")
    private LocalDate firstDueDate;

    @Min(value = 0, message = "Grace days cannot be negative")
    @Max(value = 30, message = "Grace days too large")
    private Integer graceDays = 5;

    @DecimalMin(value = "0.00", inclusive = true, message = "Late fee per day cannot be negative")
    private BigDecimal lateFeePerDay = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", inclusive = true, message = "Max late fee cannot be negative")
    private BigDecimal maxLateFee = BigDecimal.ZERO;

    private String remarks;
}