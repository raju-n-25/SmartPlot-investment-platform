package com.example.spip_draft.dto.legal;

import com.example.spip_draft.enums.DcConversionStatus;
import com.example.spip_draft.enums.DocumentStatus;
import com.example.spip_draft.enums.ReraStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectLegalDetailsRequest {

    private String reraRegistrationNumber;

    @NotNull(message = "RERA status is required")
    private ReraStatus reraStatus;

    @NotNull(message = "DC conversion status is required")
    private DcConversionStatus dcConversionStatus;

    @NotNull(message = "Document status is required")
    private DocumentStatus documentStatus;

    private String legalNotes;
}