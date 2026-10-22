package com.example.spip_draft.controller;

import com.example.spip_draft.service.PlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/plots")
@RequiredArgsConstructor
public class AdminPlotController {

    private final PlotService plotService;

    /**
     * Approve plot
     */
    @PutMapping("/{plotId}/approve")
    public ResponseEntity<String> approvePlot(@PathVariable Long plotId) {
        plotService.approvePlot(plotId);
        return ResponseEntity.ok("Plot approved successfully");
    }

    /**
     * Reject plot
     */
    @PutMapping("/{plotId}/reject")
    public ResponseEntity<String> rejectPlot(@PathVariable Long plotId) {
        plotService.rejectPlot(plotId);
        return ResponseEntity.ok("Plot rejected successfully");
    }
}