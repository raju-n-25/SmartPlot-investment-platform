package com.example.spip_draft.opencv.service;

import com.example.spip_draft.entity.Project;
import com.example.spip_draft.opencv.config.OpenCvProperties;
import com.example.spip_draft.opencv.dto.DetectedPlotDto;
import com.example.spip_draft.opencv.dto.LayoutProcessingResponse;
import com.example.spip_draft.opencv.dto.LayoutUploadResponse;
import com.example.spip_draft.opencv.entity.DetectedPlot;
import com.example.spip_draft.opencv.entity.LayoutFile;
import com.example.spip_draft.opencv.entity.LayoutProcessingResult;
import com.example.spip_draft.opencv.enums.LayoutProcessingStatus;
import com.example.spip_draft.opencv.repository.DetectedPlotRepository;
import com.example.spip_draft.opencv.repository.LayoutFileRepository;
import com.example.spip_draft.opencv.repository.LayoutProcessingResultRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutProcessingService {

    private final ProjectRepository projectRepository;
    private final LayoutFileRepository layoutFileRepository;
    private final LayoutProcessingResultRepository resultRepository;
    private final DetectedPlotRepository detectedPlotRepository;
    private final LayoutStorageService storageService;
    private final OpenCvPipelineService pipelineService;
    private final OpenCvProperties props;

    @Transactional
    public LayoutUploadResponse uploadLayout(Long projectId, MultipartFile file) {
        Project project = getAccessibleProject(projectId);

        Path storedPath = storageService.storeLayoutFile(projectId, file);
        Path convertedPath = storageService.convertPdfToImageIfNeeded(storedPath, file.getContentType());

        LayoutFile layoutFile = LayoutFile.builder()
                .project(project)
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedPath.getFileName().toString())
                .filePath(storedPath.toString())
                .convertedImagePath(convertedPath != null ? convertedPath.toString() : null)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .processingStatus(LayoutProcessingStatus.UPLOADED)
                .processingError(null)
                .build();

        LayoutFile saved = layoutFileRepository.save(layoutFile);

        log.info("[LayoutService] Layout uploaded: id={}, project={}", saved.getId(), projectId);

        return LayoutUploadResponse.builder()
                .layoutFileId(saved.getId())
                .projectId(projectId)
                .originalFileName(saved.getOriginalFileName())
                .contentType(saved.getContentType())
                .fileSize(saved.getFileSize())
                .processingStatus(saved.getProcessingStatus())
                .message("Layout file uploaded successfully. Call /process to start detection.")
                .build();
    }

    @Transactional
    public LayoutProcessingResponse processLayout(Long projectId,
                                                  Long layoutFileId,
                                                  Double scaleFactorPxPerSqft) {

        LayoutFile layoutFile = getAccessibleLayoutFile(projectId, layoutFileId);

        LayoutProcessingResponse response =
                pipelineService.process(layoutFile, scaleFactorPxPerSqft);

        log.info("[LayoutService] Processing complete: layoutFileId={}, plots={}",
                layoutFileId,
                response.getTotalPlotsDetected() != null ? response.getTotalPlotsDetected() : 0);

        return response;
    }

    @Transactional(readOnly = true)
    public LayoutProcessingResponse getResult(Long projectId, Long layoutFileId) {

        LayoutFile layoutFile = getAccessibleLayoutFile(projectId, layoutFileId);

        LayoutProcessingResult processingResult = resultRepository.findByLayoutFileId(layoutFileId)
                .orElseThrow(() -> new RuntimeException(
                        "No processing result for layoutFileId=" + layoutFileId + ". Run /process first."));

        List<DetectedPlotDto> plots = detectedPlotRepository
                .findByProcessingResultIdOrderByPlotIndexAsc(processingResult.getId())
                .stream()
                .map(this::toDto)
                .toList();

        return buildResponse(layoutFile, processingResult, plots);
    }

    @Transactional(readOnly = true)
    public List<LayoutUploadResponse> getLayoutsForProject(Long projectId) {

        Project project = getAccessibleProject(projectId);

        return layoutFileRepository.findByProjectIdOrderByCreatedAtDesc(project.getId())
                .stream()
                .map(lf -> LayoutUploadResponse.builder()
                        .layoutFileId(lf.getId())
                        .projectId(projectId)
                        .originalFileName(lf.getOriginalFileName())
                        .contentType(lf.getContentType())
                        .fileSize(lf.getFileSize())
                        .processingStatus(lf.getProcessingStatus())
                        .message(lf.getProcessingError())
                        .build())
                .toList();
    }

    private DetectedPlotDto toDto(DetectedPlot e) {
        return DetectedPlotDto.builder()
                .id(e.getId())
                .plotIndex(e.getPlotIndex())
                .bboxX(e.getBboxX())
                .bboxY(e.getBboxY())
                .bboxWidth(e.getBboxWidth())
                .bboxHeight(e.getBboxHeight())
                .pixelArea(e.getPixelArea())
                .areaSqft(e.getAreaSqft())
                .areaSqm(e.getAreaSqm())
                .isRectangle(e.getIsRectangle())
                .cornerPointsJson(e.getCornerPointsJson())
                .linkedPlotId(e.getLinkedPlot() != null ? e.getLinkedPlot().getId() : null)
                .build();
    }

    private LayoutProcessingResponse buildResponse(LayoutFile lf,
                                                   LayoutProcessingResult r,
                                                   List<DetectedPlotDto> plots) {
        return LayoutProcessingResponse.builder()
                .layoutFileId(lf.getId())
                .processingResultId(r.getId())
                .projectId(lf.getProject().getId())
                .processingStatus(lf.getProcessingStatus())
                .processingError(lf.getProcessingError())
                .totalPlotsDetected(r.getTotalPlotsDetected())
                .scaleFactorPxPerSqft(r.getScaleFactorPxPerSqft())
                .debugGrayscalePath(r.getDebugGrayscalePath())
                .debugEdgePath(r.getDebugEdgePath())
                .debugContourPath(r.getDebugContourPath())
                .debugFilteredPlotsPath(r.getDebugFilteredPlotsPath())
                .imageWidth(r.getImageWidth())
                .imageHeight(r.getImageHeight())
                .detectedPlots(plots)
                .build();
    }

    private Project getAccessibleProject(Long projectId) {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Only builder users can access layout processing.");
        }

        return projectRepository.findByIdAndBuilderId(projectId, builderId)
                .orElseThrow(() -> new RuntimeException(
                        "Project not found or you do not have access to this project: id=" + projectId));
    }

    private LayoutFile getAccessibleLayoutFile(Long projectId, Long layoutFileId) {
        Project project = getAccessibleProject(projectId);

        LayoutFile layoutFile = layoutFileRepository.findById(layoutFileId)
                .orElseThrow(() -> new RuntimeException("Layout file not found: id=" + layoutFileId));

        if (!layoutFile.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("Layout file does not belong to the specified project.");
        }

        return layoutFile;
    }
}