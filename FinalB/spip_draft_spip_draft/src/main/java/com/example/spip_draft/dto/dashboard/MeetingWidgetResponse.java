package com.example.spip_draft.dto.dashboard;

import com.example.spip_draft.enums.MeetingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class MeetingWidgetResponse {
    private Long meetingId;
    private Long investorId;
    private Long builderId;
    private Long projectId;
    private Long plotId;

    private BigDecimal budget;
    private String expectedTimeline;
    private String purpose;

    private MeetingStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
}