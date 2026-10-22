	package com.example.spip_draft.entity;
	
	import com.example.spip_draft.enums.EmiPaymentMode;
	import com.example.spip_draft.enums.EmiPaymentTransactionStatus;
	import jakarta.persistence.*;
	import lombok.Getter;
	import lombok.Setter;
	
	import java.math.BigDecimal;
	import java.time.LocalDateTime;
	
	@Entity
	@Table(name = "emi_payment_transactions",
	        indexes = {
	                @Index(name = "idx_emi_tx_installment", columnList = "emi_installment_id"),
	                @Index(name = "idx_emi_tx_plan", columnList = "emi_plan_id"),
	                @Index(name = "idx_emi_tx_investor", columnList = "investor_id"),
	                @Index(name = "idx_emi_tx_status", columnList = "status")
	        })
	@Getter
	@Setter
	public class EmiPaymentTransaction extends BaseEntity {
	
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "emi_plan_id", nullable = false)
	    private EmiPlan emiPlan;
	
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "emi_installment_id", nullable = false)
	    private EmiInstallment emiInstallment;
	
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "investor_id", nullable = false)
	    private Investor investor;
	
	    @Column(name = "tenant_id", nullable = false)
	    private Long tenantId;
	
	    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
	    private BigDecimal amount;
	
	    @Column(name = "wallet_amount", nullable = false, precision = 15, scale = 2)
	    private BigDecimal walletAmount = BigDecimal.ZERO;
	
	    @Column(name = "gateway_amount", nullable = false, precision = 15, scale = 2)
	    private BigDecimal gatewayAmount = BigDecimal.ZERO;
	
	    @Enumerated(EnumType.STRING)
	    @Column(name = "payment_mode", nullable = false, length = 20)
	    private EmiPaymentMode paymentMode;
	
	    @Enumerated(EnumType.STRING)
	    @Column(name = "status", nullable = false, length = 20)
	    private EmiPaymentTransactionStatus status = EmiPaymentTransactionStatus.INITIATED;
	
	    @Column(name = "gateway_type", length = 50)
	    private String gatewayType;
	
	    @Column(name = "gateway_reference", length = 150)
	    private String gatewayReference;
	
	    @Column(name = "paid_at")
	    private LocalDateTime paidAt;
	
	    @Column(name = "remarks", length = 500)
	    private String remarks;
	}