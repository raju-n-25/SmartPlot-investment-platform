package com.example.spip_draft.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentOptionRequest {

    @NotBlank(message = "Option is required")
    private String option; // FULL_PAYMENT or EMI

    private String note;
}