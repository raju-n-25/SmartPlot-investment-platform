package com.example.spip_draft.controller;

import com.example.spip_draft.dto.common.ApiResponse;
import com.example.spip_draft.dto.marketplace.PlotFilterRequest;
import com.example.spip_draft.dto.marketplace.PlotResponse;
import com.example.spip_draft.mapper.PlotMapper;
import com.example.spip_draft.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    // =========================
    // 🌍 ALL PLOTS
    // =========================
    @GetMapping("/plots")
    public ResponseEntity<ApiResponse<List<PlotResponse>>> getPlots() {

        List<PlotResponse> response = marketplaceService.getAllPlots()
                .stream()
                .map(PlotMapper::toResponse)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success("Available plots fetched successfully", response)
        );
    }

    // =========================
    // 📄 SINGLE PLOT
    // =========================
    @GetMapping("/plots/{id}")
    public ResponseEntity<ApiResponse<PlotResponse>> getPlot(@PathVariable Long id) {

        PlotResponse response = PlotMapper.toResponse(
                marketplaceService.getPlot(id)
        );

        return ResponseEntity.ok(
                ApiResponse.success("Plot details fetched successfully", response)
        );
    }

    // =========================
    // 🔍 FILTER
    // =========================
    @PostMapping("/plots/filter")
    public ResponseEntity<ApiResponse<List<PlotResponse>>> filterPlots(
            @RequestBody PlotFilterRequest request) {

        List<PlotResponse> response = marketplaceService.filterPlots(request)
                .stream()
                .map(PlotMapper::toResponse)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success("Filtered plots fetched successfully", response)
        );
    }

    // =========================
    // ⚖ COMPARE
    // =========================
    @PostMapping("/plots/compare")
    public ResponseEntity<ApiResponse<List<PlotResponse>>> comparePlots(
            @RequestBody List<Long> plotIds) {

        List<PlotResponse> response = marketplaceService.comparePlots(plotIds)
                .stream()
                .map(PlotMapper::toResponse)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success("Plots compared successfully", response)
        );
    }
}