package com.example.spip_draft.entity;

import com.example.spip_draft.enums.MeetingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_requests")
@Getter
@Setter
public class MeetingRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "builder_id", nullable = false)
    private Builder builder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id")
    private Plot plot;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "expected_timeline")
    private String expectedTimeline;

    @Column(length = 1000)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.REQUESTED;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent = false;

    // ✅ NEW
    @Column(name = "from_marketplace", nullable = false)
    private Boolean fromMarketplace = false;

    @Column(name = "hold_active", nullable = false)
    private Boolean holdActive = false;

    @Column(name = "meeting_round", nullable = false)
    private Integer meetingRound = 1;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reason", length = 500)
    private String releaseReason;

    @Column(name = "bought_from_meeting", nullable = false)
    private Boolean boughtFromMeeting = false;
    
    @Column(name = "requested_reschedule_at")
    private LocalDateTime requestedRescheduleAt;

    @Column(name = "reschedule_reason", length = 1000)
    private String rescheduleReason;

    @Column(name = "reschedule_requested_at")
    private LocalDateTime rescheduleRequestedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "from_booking", nullable = false)
    private Boolean fromBooking = false;
}