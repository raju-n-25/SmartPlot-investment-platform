 package com.example.spip_draft.dto.meeting;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateMeetingRequest {

	@NotNull
    private Long builderId;

    private Long projectId;
    private Long plotId;

    private Long bookingId;

    private BigDecimal budget;
    private String expectedTimeline;
    private String purpose;

    private Boolean fromMarketplace = false;
    private Boolean fromBooking = false;
}




























