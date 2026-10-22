package com.example.spip_draft.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminConflictLogResponse {
    private Long id;
    private Long bookingId;
    private Long plotId;
    private String message;
    private LocalDateTime createdAt;
}