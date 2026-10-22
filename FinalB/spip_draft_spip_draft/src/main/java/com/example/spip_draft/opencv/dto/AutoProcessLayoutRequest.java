package com.example.spip_draft.opencv.dto;

import lombok.Data;

/**
 * Optional request body for the one-shot upload+detect+persist endpoint.
 * All fields are optional — defaults are applied when absent.
 */
@Data
public class AutoProcessLayoutRequest {

    /**
     * Pixels per square foot scale factor for this image.
     * If absent, the system default (application.properties) is used.
     */
    private Double scaleFactorPxPerSqft;

    /**
     * When true (default), detected plots are persisted as Plot entities.
     * Set to false for a dry-run preview.
     */
    private Boolean persist = true;

    /**
     * Pixel tolerance for road/boundary detection.
     * Default: 10 px.
     */
    private Double boundaryTolerancePx = 10.0;
}