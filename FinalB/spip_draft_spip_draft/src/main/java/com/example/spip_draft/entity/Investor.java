package com.example.spip_draft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "investors")
@Getter
@Setter
public class Investor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "pan_card", nullable = false, unique = true)
    private String panCard;

    // ✅ FIXED (BigDecimal for money)
    @Column(name = "wallet_balance", precision = 12, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Column(nullable = false)
    private boolean approved = false;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}