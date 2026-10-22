package com.example.spip_draft.controller;

import com.example.spip_draft.dto.common.ApiResponse;
import com.example.spip_draft.dto.marketplace.PlotCreateRequest;
import com.example.spip_draft.dto.marketplace.PlotResponse;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.mapper.PlotMapper;
import com.example.spip_draft.service.PlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/builder/projects/{projectId}/plots")
@RequiredArgsConstructor
public class PlotController {

    private final PlotService plotService;

    // =========================
    // ➕ Add single plot
    // =========================
    @PostMapping
    public ResponseEntity<ApiResponse<PlotResponse>> addPlot(
            @PathVariable Long projectId,
            @Valid @RequestBody PlotCreateRequest request) {

        Plot plot = plotService.addPlot(projectId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Plot created successfully",
                        PlotMapper.toResponse(plot)
                )
        );
    }

    // =========================
    // ➕ Add multiple plots
    // =========================
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<PlotResponse>>> addPlots(
            @PathVariable Long projectId,
            @Valid @RequestBody List<PlotCreateRequest> requests) {

    	List<PlotResponse> response = plotService.addPlotsBulk(projectId, requests)
    	        .stream()
    	        .map(PlotMapper::toResponse)
    	        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Plots created successfully",
                        response
                )
        );
    }

    // =========================
    // 📄 Get all plots in project
    // =========================
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlotResponse>>> getPlots(
            @PathVariable Long projectId) {

        List<PlotResponse> response = plotService.getPlotsByProject(projectId)
                .stream()
                .map(PlotMapper::toResponse)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Plots fetched successfully",
                        response
                )
        );
    }

    // =========================
    // 📄 Get single plot (SECURE)
    // =========================
    @GetMapping("/{plotId}")
    public ResponseEntity<ApiResponse<PlotResponse>> getPlot(
            @PathVariable Long projectId,
            @PathVariable Long plotId) {

        Plot plot = plotService.getPlot(projectId, plotId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Plot fetched successfully",
                        PlotMapper.toResponse(plot)
                )
        );
    }

    // =========================
    // ❌ Delete plot (SECURE)
    // =========================
    @DeleteMapping("/{plotId}")
    public ResponseEntity<ApiResponse<Void>> deletePlot(
            @PathVariable Long projectId,
            @PathVariable Long plotId) {

        plotService.deletePlot(projectId, plotId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Plot deleted successfully",
                        null
                )
        );
    }
    
 // =========================
 // ✏️ Update plot (SECURE)
 // =========================
 @PutMapping("/{plotId}")
 public ResponseEntity<ApiResponse<PlotResponse>> updatePlot(
         @PathVariable Long projectId,
         @PathVariable Long plotId,
         @Valid @RequestBody PlotCreateRequest request) {

     Plot plot = plotService.updatePlot(projectId, plotId, request);

     return ResponseEntity.ok(
             ApiResponse.success(
                     "Plot updated successfully",
                     PlotMapper.toResponse(plot)
             )
     );
 }
}