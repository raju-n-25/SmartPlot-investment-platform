package com.example.spip_draft.dto.meeting;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MeetingApprovalRequest {

    @NotNull
    private Boolean approved;

    private LocalDateTime scheduledAt;
    private String meetingLink;
    private String rejectionReason;
}