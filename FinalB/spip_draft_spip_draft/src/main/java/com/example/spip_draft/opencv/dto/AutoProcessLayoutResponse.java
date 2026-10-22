package com.example.spip_draft.opencv.dto;

import com.example.spip_draft.opencv.enums.LayoutProcessingStatus;
import com.example.spip_draft.plotintelligence.dto.PlotIntelligenceResponse;
import lombok.Builder;
import lombok.Data;

/**
 * Combined response from the one-shot upload → detect → persist pipeline.
 */
@Data
@Builder
public class AutoProcessLayoutResponse {

    private Long layoutFileId;
    private Long processingResultId;
    private Long projectId;
    private String originalFileName;
    private LayoutProcessingStatus processingStatus;
    private Integer totalPlotsDetected;

    private String debugGrayscalePath;
    private String debugEdgePath;
    private String debugContourPath;
    private String debugFilteredPlotsPath;

    /** Detailed intelligence results per plot (null when persist=false preview) */
    private PlotIntelligenceResponse intelligenceResult;

    private String message;
}