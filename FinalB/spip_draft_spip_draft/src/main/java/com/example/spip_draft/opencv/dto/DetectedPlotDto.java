package com.example.spip_draft.opencv.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO representing a single plot detected by the OpenCV pipeline.
 */
@Data
@Builder
public class DetectedPlotDto {

    private Long id;
    private Integer plotIndex;

    private Integer bboxX;
    private Integer bboxY;
    private Integer bboxWidth;
    private Integer bboxHeight;

    private Double pixelArea;
    private Double areaSqft;
    private Double areaSqm;

    /** True when contour was approximated to a 4-sided polygon */
    private Boolean isRectangle;

    /**
     * JSON array of corner points, e.g.
     * [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]
     */
    private String cornerPointsJson;

    /** ID of the Plot entity this detected plot is linked to (nullable) */
    private Long linkedPlotId;
}