package com.example.spip_draft.entity;

import com.example.spip_draft.enums.EmiPlanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "emi_plans",
        indexes = {
                @Index(name = "idx_emi_plan_booking", columnList = "booking_id"),
                @Index(name = "idx_emi_plan_investor", columnList = "investor_id"),
                @Index(name = "idx_emi_plan_tenant", columnList = "tenant_id"),
                @Index(name = "idx_emi_plan_status", columnList = "status")
        })
@Getter
@Setter
public class EmiPlan extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "token_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal tokenPaid = BigDecimal.ZERO;

    @Column(name = "down_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal downPayment = BigDecimal.ZERO;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "annual_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "monthly_emi", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyEmi;

    @Column(name = "total_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Column(name = "total_payable", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "paid_installments", nullable = false)
    private Integer paidInstallments = 0;

    @Column(name = "total_installments", nullable = false)
    private Integer totalInstallments;

    @Column(name = "remaining_principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingPrincipal;

    @Column(name = "grace_days", nullable = false)
    private Integer graceDays = 5;

    @Column(name = "late_fee_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFeePerDay = BigDecimal.ZERO;

    @Column(name = "max_late_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxLateFee = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmiPlanStatus status = EmiPlanStatus.ACTIVE;

    @Column(name = "remarks", length = 500)
    private String remarks;
}