package com.example.spip_draft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_conflicts")
@Getter
@Setter
public class BookingConflictLog extends BaseEntity {

    @Column(name = "plot_id")
    private Long plotId;

    @Column(name = "investor_id")
    private Long investorId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "conflict_type")
    private String conflictType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "attempt_time")
    private LocalDateTime attemptTime;
}