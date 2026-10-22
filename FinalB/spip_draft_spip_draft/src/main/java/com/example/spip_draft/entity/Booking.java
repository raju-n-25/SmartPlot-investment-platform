package com.example.spip_draft.entity;

import com.example.spip_draft.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    @Column
    private LocalDateTime tokenExpiry;

    @Column
    private String paymentReference;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "builder_response_at")
    private LocalDateTime builderResponseAt;

    @Column(name = "builder_response_note", length = 500)
    private String builderResponseNote;

    @Column(name = "builder_approved_by")
    private Long builderApprovedBy;
}