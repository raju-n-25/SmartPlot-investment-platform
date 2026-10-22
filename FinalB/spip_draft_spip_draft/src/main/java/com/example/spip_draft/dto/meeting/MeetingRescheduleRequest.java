package com.example.spip_draft.dto.meeting;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MeetingRescheduleRequest {

    @NotNull(message = "Requested reschedule date/time is required")
    private LocalDateTime requestedRescheduleAt;

    private String rescheduleReason;
}