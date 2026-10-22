package com.example.spip_draft.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull(message = "Plot id is required")
    private Long plotId;
}