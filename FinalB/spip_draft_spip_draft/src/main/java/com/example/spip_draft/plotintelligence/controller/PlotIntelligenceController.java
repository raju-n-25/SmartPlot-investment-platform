package com.example.spip_draft.plotintelligence.controller;

import com.example.spip_draft.dto.common.ApiResponse;

import com.example.spip_draft.plotintelligence.dto.EnrichedPlotDto;
import com.example.spip_draft.plotintelligence.dto.PlotIntelligenceRequest;
import com.example.spip_draft.plotintelligence.dto.PlotIntelligenceResponse;
import com.example.spip_draft.plotintelligence.service.PlotIntelligenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Module 7 Submodule 2 – Plot Intelligence and Integration.
 *
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │  POST  /api/builder/plot-intelligence/process                                │
 * │        Convert a batch of OpenCV contours into enriched Plot entities.       │
 * │                                                                              │
 * │  GET   /api/builder/plot-intelligence/projects/{projectId}/plots             │
 * │        Retrieve all intelligence-enriched plots for a project.               │
 * │                                                                              │
 * │  POST  /api/builder/plot-intelligence/dry-run                                │
 * │        Same as /process but with persist=false for preview/validation.       │
 * └──────────────────────────────────────────────────────────────────────────────┘
 *
 * Authentication note:
 * These endpoints sit under /api/builder/** and are therefore protected by the
 * existing {@code SecurityConfig} which requires the BUILDER role.
 */
@Slf4j
@RestController
@RequestMapping("/api/builder/plot-intelligence")
@RequiredArgsConstructor
@CrossOrigin("*")
@Tag(name = "Plot Intelligence", description = "Convert OpenCV contours into structured plot entities")
public class PlotIntelligenceController {

    private final PlotIntelligenceService plotIntelligenceService;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. PROCESS (persist = true)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts a list of OpenCV contours into Plot entities stored in the DB.
     *
     * Request example:
     * <pre>{@code
     * {
     *   "projectId": 1,
     *   "imageWidth": 1200,
     *   "imageHeight": 900,
     *   "scaleFactorPxPerSqft": 100.0,
     *   "contours": [
     *     {
     *       "detectedPlotId": 42,
     *       "cornerPointsJson": "[[10,20],[110,20],[110,90],[10,90]]",
     *       "plotNumber": "A-1",
     *       "pricePerSqft": 4500.00
     *     }
     *   ]
     * }
     * }</pre>
     */
    @Operation(summary = "Convert OpenCV contours into Plot entities",
               description = "Processes each contour: validates shape, calculates area (Shoelace), "
                           + "detects road/facing direction, and persists as a Plot entity linked to the project.")
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PlotIntelligenceResponse>> processContours(
            @Valid @RequestBody PlotIntelligenceRequest request) {

        log.info("[PlotIntelligenceController] Process request: project={}, contours={}",
                request.getProjectId(), request.getContours().size());

        // Force persist = true for this endpoint
        request.setPersist(true);

        PlotIntelligenceResponse response = plotIntelligenceService.processContours(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Plot intelligence processing complete. " + response.getPersisted() + " plot(s) created.",
                        response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. DRY RUN (persist = false)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Same processing pipeline as {@code /process} but nothing is saved to the
     * database. Use this to preview area calculations and facing detection before
     * committing.
     */
    @Operation(summary = "Dry-run: preview plot intelligence without persisting",
               description = "Runs the full pipeline and returns enriched plot data, "
                           + "but does NOT create any Plot entities in the database.")
    @PostMapping("/dry-run")
    public ResponseEntity<ApiResponse<PlotIntelligenceResponse>> dryRun(
            @Valid @RequestBody PlotIntelligenceRequest request) {

        log.info("[PlotIntelligenceController] Dry-run: project={}, contours={}",
                request.getProjectId(), request.getContours().size());

        // Force dry-run mode regardless of what the caller sent
        request.setPersist(false);

        PlotIntelligenceResponse response = plotIntelligenceService.processContours(request);

        return ResponseEntity.ok(ApiResponse.success(
                "Dry run complete. No plots were persisted.", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. GET enriched plots for a project
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all plots belonging to a project, enriched with intelligence
     * fields (shape type, normalised coordinates, facing direction, road flags).
     *
     * This is a read-only endpoint — it fetches what was previously persisted
     * by the {@code /process} call.
     */
    @Operation(summary = "Get all enriched plots for a project",
               description = "Returns Plot entities for a project with all intelligence "
                           + "fields (shape, area, facing, road flags) populated.")
    @GetMapping("/projects/{projectId}/plots")
    public ResponseEntity<ApiResponse<List<EnrichedPlotDto>>> getEnrichedPlots(
            @PathVariable Long projectId) {

        log.info("[PlotIntelligenceController] Fetching enriched plots for project={}", projectId);

        List<EnrichedPlotDto> plots = plotIntelligenceService.getEnrichedPlotsForProject(projectId);

        return ResponseEntity.ok(ApiResponse.success(
                "Enriched plots fetched. Count: " + plots.size(), plots));
    }
}
