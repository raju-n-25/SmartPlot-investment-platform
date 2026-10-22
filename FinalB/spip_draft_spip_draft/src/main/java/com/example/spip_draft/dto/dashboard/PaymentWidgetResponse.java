package com.example.spip_draft.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentWidgetResponse {
    private Long paymentId;
    private Long bookingId;
    private Long plotId;
    private Long investorId;

    private BigDecimal amount;
    private String paymentStatus;
    private Boolean escrowReleased;
    private LocalDateTime createdAt;
}