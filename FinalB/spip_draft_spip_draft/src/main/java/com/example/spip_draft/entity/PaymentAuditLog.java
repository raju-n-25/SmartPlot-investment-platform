package com.example.spip_draft.entity;

import com.example.spip_draft.enums.PaymentAuditEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_audit_logs",
        indexes = {
                @Index(name = "idx_audit_payment", columnList = "payment_id"),
                @Index(name = "idx_audit_booking", columnList = "booking_id"),
                @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
                @Index(name = "idx_audit_event", columnList = "event_type")
        })
@Getter
@Setter
public class PaymentAuditLog extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private PaymentAuditEventType eventType;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "actor_type", length = 50)
    private String actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}