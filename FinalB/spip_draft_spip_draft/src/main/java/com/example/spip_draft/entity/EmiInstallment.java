package com.example.spip_draft.entity;

import com.example.spip_draft.enums.InstallmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emi_installments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plan_installment_no", columnNames = {"emi_plan_id", "installment_number"})
        },
        indexes = {
                @Index(name = "idx_emi_inst_plan", columnList = "emi_plan_id"),
                @Index(name = "idx_emi_inst_due_date", columnList = "due_date"),
                @Index(name = "idx_emi_inst_status", columnList = "status")
        })
@Getter
@Setter
public class EmiInstallment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emi_plan_id", nullable = false)
    private EmiPlan emiPlan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "opening_principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingPrincipal;

    @Column(name = "principal_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestComponent;

    @Column(name = "emi_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "principal_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "interest_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "penalty_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyPaid = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingDue;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "last_payment_at")
    private LocalDateTime lastPaymentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "remarks", length = 500)
    private String remarks;
}