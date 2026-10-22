package com.example.spip_draft.opencv.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/**
 * Request body for the "process layout" endpoint.
 * The scale factor controls conversion from pixel area to sqft.
 */
@Data
public class ProcessLayoutRequest {

    @DecimalMin(value = "0.0001", message = "Scale factor must be > 0")
    private Double scaleFactorPxPerSqft;
}