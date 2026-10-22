package com.example.spip_draft.opencv.service;

import com.example.spip_draft.opencv.dto.AutoProcessLayoutResponse;
import com.example.spip_draft.opencv.dto.LayoutProcessingResponse;
import com.example.spip_draft.opencv.dto.LayoutUploadResponse;
import com.example.spip_draft.opencv.entity.DetectedPlot;
import com.example.spip_draft.opencv.repository.DetectedPlotRepository;
import com.example.spip_draft.plotintelligence.dto.ContourInputDto;
import com.example.spip_draft.plotintelligence.dto.PlotIntelligenceRequest;
import com.example.spip_draft.plotintelligence.dto.PlotIntelligenceResponse;
import com.example.spip_draft.plotintelligence.service.PlotIntelligenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoLayoutProcessingService {

    private final LayoutProcessingService layoutProcessingService;
    private final PlotIntelligenceService plotIntelligenceService;
    private final DetectedPlotRepository detectedPlotRepository;

    public AutoProcessLayoutResponse runFullPipeline(Long projectId,
                                                     MultipartFile file,
                                                     Double scaleFactorPxPerSqft,
                                                     boolean persist,
                                                     double boundaryTolerancePx) {

        log.info("[AutoPipeline] START — project={}, file={}, persist={}",
                projectId, file.getOriginalFilename(), persist);

        LayoutUploadResponse uploadResp = layoutProcessingService.uploadLayout(projectId, file);
        Long layoutFileId = uploadResp.getLayoutFileId();
        log.info("[AutoPipeline] Step 1 DONE — layoutFileId={}", layoutFileId);

        LayoutProcessingResponse detectionResp =
                layoutProcessingService.processLayout(projectId, layoutFileId, scaleFactorPxPerSqft);

        int detected = detectionResp.getTotalPlotsDetected() != null
                ? detectionResp.getTotalPlotsDetected() : 0;
        log.info("[AutoPipeline] Step 2 DONE — {} plots detected", detected);

        if (detected == 0) {
            log.warn("[AutoPipeline] No plots detected — skipping intelligence step");
            return AutoProcessLayoutResponse.builder()
                    .layoutFileId(layoutFileId)
                    .processingResultId(detectionResp.getProcessingResultId())
                    .projectId(projectId)
                    .originalFileName(file.getOriginalFilename())
                    .processingStatus(detectionResp.getProcessingStatus())
                    .totalPlotsDetected(0)
                    .debugGrayscalePath(detectionResp.getDebugGrayscalePath())
                    .debugEdgePath(detectionResp.getDebugEdgePath())
                    .debugContourPath(detectionResp.getDebugContourPath())
                    .debugFilteredPlotsPath(detectionResp.getDebugFilteredPlotsPath())
                    .message("No plots detected in layout image. Try adjusting scale factor or check image quality.")
                    .build();
        }

        List<DetectedPlot> savedDetectedPlots =
                detectedPlotRepository.findByProcessingResultIdOrderByPlotIndexAsc(
                        detectionResp.getProcessingResultId());

        List<ContourInputDto> contours = savedDetectedPlots.stream()
                .map(dp -> ContourInputDto.builder()
                        .detectedPlotId(dp.getId())
                        .cornerPointsJson(dp.getCornerPointsJson())
                        .build())
                .collect(Collectors.toList());

        Integer imgWidth = (detectionResp.getImageWidth() != null && detectionResp.getImageWidth() > 0)
                ? detectionResp.getImageWidth()
                : inferImageWidth(savedDetectedPlots);

        Integer imgHeight = (detectionResp.getImageHeight() != null && detectionResp.getImageHeight() > 0)
                ? detectionResp.getImageHeight()
                : inferImageHeight(savedDetectedPlots);

        PlotIntelligenceRequest intelligenceReq = PlotIntelligenceRequest.builder()
                .projectId(projectId)
                .contours(contours)
                .imageWidth(imgWidth)
                .imageHeight(imgHeight)
                .scaleFactorPxPerSqft(scaleFactorPxPerSqft)
                .persist(persist)
                .boundaryTolerancePx(boundaryTolerancePx)
                .build();

        PlotIntelligenceResponse intelligenceResp =
                plotIntelligenceService.processContours(intelligenceReq);

        log.info("[AutoPipeline] Step 3 DONE — accepted={}, persisted={}",
                intelligenceResp.getAccepted(), intelligenceResp.getPersisted());

        String msg = persist
                ? String.format("Pipeline complete. Detected %d plots, persisted %d Plot entities.",
                detected, intelligenceResp.getPersisted())
                : String.format("Dry-run complete. Detected %d plots; no entities persisted.", detected);

        return AutoProcessLayoutResponse.builder()
                .layoutFileId(layoutFileId)
                .processingResultId(detectionResp.getProcessingResultId())
                .projectId(projectId)
                .originalFileName(file.getOriginalFilename())
                .processingStatus(detectionResp.getProcessingStatus())
                .totalPlotsDetected(detected)
                .debugGrayscalePath(detectionResp.getDebugGrayscalePath())
                .debugEdgePath(detectionResp.getDebugEdgePath())
                .debugContourPath(detectionResp.getDebugContourPath())
                .debugFilteredPlotsPath(detectionResp.getDebugFilteredPlotsPath())
                .intelligenceResult(intelligenceResp)
                .message(msg)
                .build();
    }

    private Integer inferImageWidth(List<DetectedPlot> plots) {
        return (int) (plots.stream()
                .mapToDouble(dp -> (dp.getBboxX() != null ? dp.getBboxX() : 0)
                        + (dp.getBboxWidth() != null ? dp.getBboxWidth() : 0))
                .max()
                .orElse(1000.0) + 50.0);
    }

    private Integer inferImageHeight(List<DetectedPlot> plots) {
        return (int) (plots.stream()
                .mapToDouble(dp -> (dp.getBboxY() != null ? dp.getBboxY() : 0)
                        + (dp.getBboxHeight() != null ? dp.getBboxHeight() : 0))
                .max()
                .orElse(1000.0) + 50.0);
    }
}