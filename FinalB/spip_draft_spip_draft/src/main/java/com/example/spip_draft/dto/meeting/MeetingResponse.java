package com.example.spip_draft.dto.meeting;

import com.example.spip_draft.enums.MeetingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class MeetingResponse {
    private Long id;

    private Long investorId;
    private Long builderId;
    private Long projectId;
    private Long plotId;
    private Long bookingId;

    private String investorName;
    private String builderName;
    private String projectName;
    private String plotNumber;

    private BigDecimal budget;
    private String expectedTimeline;
    private String purpose;

    private MeetingStatus status;
    private LocalDateTime scheduledAt;
    private String meetingLink;
    private String rejectionReason;

    private Boolean reminderSent;
    private Boolean fromMarketplace;
    private Boolean fromBooking;
    private Boolean holdActive;
    private Integer meetingRound;
    private Boolean boughtFromMeeting;
    private LocalDateTime releasedAt;
    private String releaseReason;

    private Boolean canRequestFollowUp;
    private Boolean canProceedToBuy;
    private Boolean canReleasePlot;
    private Boolean canBuilderRejectAfterMeet;

    private LocalDateTime createdAt;

    private LocalDateTime requestedRescheduleAt;
    private String rescheduleReason;
    private LocalDateTime rescheduleRequestedAt;

    private Boolean canRequestReschedule;
    private Boolean canApproveReschedule;
}