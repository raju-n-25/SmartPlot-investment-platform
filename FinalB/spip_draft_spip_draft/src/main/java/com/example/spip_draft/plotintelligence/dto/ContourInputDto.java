package com.example.spip_draft.plotintelligence.dto;

import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContourInputDto {

    /** Back-link to detected_plots row */
    private Long detectedPlotId;

    /** Suggested plot number from upstream */
    private String plotNumber;

    /** Structured polygon points */
    @Valid
    @Builder.Default
    private List<ContourPoint> points = new ArrayList<>();

    /** Raw JSON contour points, e.g. [[x1,y1],[x2,y2],...] */
    private String cornerPointsJson;

    /** Optional bbox fallback */
    private Double bboxX;
    private Double bboxY;
    private Double bboxWidth;
    private Double bboxHeight;

    /** Optional precomputed pixel area */
    private Double pixelArea;

    /** Optional rectangle hint */
    private Boolean rectangleCandidate;

    /** Manual facing override */
    private String manualFacingDirection;

    /** Optional pricing */
    private BigDecimal pricePerSqft;
}