package com.example.spip_draft.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentActivityDTO {

    private String type;        // BOOKING / MEETING / PAYMENT
    private String description; // human readable
    private LocalDateTime time;
    private String status;
}