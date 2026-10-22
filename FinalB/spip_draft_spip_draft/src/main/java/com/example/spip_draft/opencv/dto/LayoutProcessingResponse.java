package com.example.spip_draft.opencv.dto;

import com.example.spip_draft.opencv.enums.LayoutProcessingStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Full response returned after the OpenCV pipeline completes.
 * Includes aggregated stats, debug image paths, and per-plot details.
 */
@Data
@Builder
public class LayoutProcessingResponse {

    private Long layoutFileId;
    private Long processingResultId;
    private Long projectId;

    private LayoutProcessingStatus processingStatus;
    private String processingError;

    private Integer totalPlotsDetected;
    private Double scaleFactorPxPerSqft;

    private String debugGrayscalePath;
    private String debugEdgePath;
    private String debugContourPath;
    private String debugFilteredPlotsPath;

    private Integer imageWidth;
    private Integer imageHeight;

    private List<DetectedPlotDto> detectedPlots;
}