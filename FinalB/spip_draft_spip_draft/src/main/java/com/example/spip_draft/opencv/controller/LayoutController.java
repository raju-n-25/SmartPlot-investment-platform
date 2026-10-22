package com.example.spip_draft.opencv.controller;

import com.example.spip_draft.dto.common.ApiResponse;
import com.example.spip_draft.opencv.dto.AutoProcessLayoutResponse;
import com.example.spip_draft.opencv.dto.LayoutProcessingResponse;
import com.example.spip_draft.opencv.dto.LayoutUploadResponse;
import com.example.spip_draft.opencv.dto.ProcessLayoutRequest;
import com.example.spip_draft.opencv.service.AutoLayoutProcessingService;
import com.example.spip_draft.opencv.service.LayoutProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for Module 7 – OpenCV Layout Processing.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  STEP-BY-STEP endpoints (manual flow):
 *
 *  POST   /api/builder/projects/{projectId}/layouts/upload
 *         → Upload layout image/PDF, store on disk, return layoutFileId.
 *
 *  POST   /api/builder/projects/{projectId}/layouts/{layoutFileId}/process
 *         → Run OpenCV detection pipeline (BFS flood-fill, filtering).
 *
 *  GET    /api/builder/projects/{projectId}/layouts/{layoutFileId}/result
 *         → Fetch stored detection result with detected plot list.
 *
 *  GET    /api/builder/projects/{projectId}/layouts
 *         → List all uploaded layouts for a project.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  ONE-SHOT endpoint (auto flow):
 *
 *  POST   /api/builder/projects/{projectId}/layouts/auto-process
 *         → Upload image + run OpenCV + run Plot Intelligence in one call.
 *         → Optional params: scaleFactorPxPerSqft, persist, boundaryTolerancePx
 *         → Returns combined detection + intelligence result with Plot IDs.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@RestController
@RequestMapping("/api/builder/projects/{projectId}/layouts")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LayoutController {

    private final LayoutProcessingService     layoutProcessingService;
    private final AutoLayoutProcessingService autoLayoutProcessingService;

    // ─────────────────────────────────────────────────────────────────────────
    // ONE-SHOT: Upload → Detect → Plot Intelligence (single API call)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * One-shot endpoint: upload a layout image, run OpenCV detection, and
     * automatically convert detected contours into Plot entities via the
     * Plot Intelligence pipeline — all in a single API call.
     *
     * <p>Request: {@code multipart/form-data}
     * <ul>
     *   <li>{@code file} (required) — JPG, PNG, or PDF layout image</li>
     *   <li>{@code scaleFactorPxPerSqft} (optional) — pixels per sq.ft, default from config</li>
     *   <li>{@code persist} (optional) — true (default) to save plots, false for dry-run</li>
     *   <li>{@code boundaryTolerancePx} (optional) — road detection tolerance, default 10px</li>
     * </ul>
     *
     * <p>Returns: combined OpenCV detection result + intelligence result with Plot IDs.
     */
    @PostMapping(value = "/auto-process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AutoProcessLayoutResponse>> autoProcess(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "scaleFactorPxPerSqft", required = false) Double scaleFactorPxPerSqft,
            @RequestParam(value = "persist", defaultValue = "false") boolean persist,
            @RequestParam(value = "boundaryTolerancePx", defaultValue = "10.0") double boundaryTolerancePx) {

        log.info("[LayoutController] Auto-process: project={}, file={}, scale={}, persist={}",
                projectId, file.getOriginalFilename(), scaleFactorPxPerSqft, persist);

        AutoProcessLayoutResponse response = autoLayoutProcessingService.runFullPipeline(
                projectId, file, scaleFactorPxPerSqft, persist, boundaryTolerancePx);

        HttpStatus status = persist ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.success(response.getMessage(), response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. UPLOAD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Upload a layout image or PDF.
     * Returns layoutFileId to use in the /process call.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LayoutUploadResponse>> uploadLayout(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {

        log.info("[LayoutController] Upload: project={}, file={}, size={}",
                projectId, file.getOriginalFilename(), file.getSize());

        LayoutUploadResponse response = layoutProcessingService.uploadLayout(projectId, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Layout uploaded successfully.", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. PROCESS (OpenCV only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Run OpenCV detection for a previously uploaded layout.
     * Optional body: {@code { "scaleFactorPxPerSqft": 120.0 }}
     */
    @PostMapping("/{layoutFileId}/process")
    public ResponseEntity<ApiResponse<LayoutProcessingResponse>> processLayout(
            @PathVariable Long projectId,
            @PathVariable Long layoutFileId,
            @Valid @RequestBody(required = false) ProcessLayoutRequest request) {

        log.info("[LayoutController] Process: project={}, layoutFileId={}", projectId, layoutFileId);

        Double scale = (request != null) ? request.getScaleFactorPxPerSqft() : null;
        LayoutProcessingResponse response = layoutProcessingService.processLayout(projectId, layoutFileId, scale);

        return ResponseEntity.ok(ApiResponse.success("Layout processed successfully.", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. FETCH RESULT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{layoutFileId}/result")
    public ResponseEntity<ApiResponse<LayoutProcessingResponse>> getResult(
            @PathVariable Long projectId,
            @PathVariable Long layoutFileId) {

    	LayoutProcessingResponse response = layoutProcessingService.getResult(projectId, layoutFileId);
        return ResponseEntity.ok(ApiResponse.success("Processing result fetched.", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. LIST UPLOADS FOR PROJECT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<LayoutUploadResponse>>> getLayoutsForProject(
            @PathVariable Long projectId) {

        List<LayoutUploadResponse> layouts = layoutProcessingService.getLayoutsForProject(projectId);
        return ResponseEntity.ok(ApiResponse.success("Layouts fetched.", layouts));
    }
}
