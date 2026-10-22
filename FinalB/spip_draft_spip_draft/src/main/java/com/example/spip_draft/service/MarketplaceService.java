package com.example.spip_draft.service;

import com.example.spip_draft.dto.marketplace.PlotFilterRequest;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.specification.PlotSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final PlotRepository plotRepository;

    // =========================
    // 🌍 GET ALL MARKETPLACE PLOTS
    // =========================
    public List<Plot> getAllPlots() {
        return plotRepository.findVisiblePlots();
    }

    // =========================
    // 📄 GET SINGLE PLOT
    // =========================
    public Plot getPlot(Long plotId) {
        Plot plot = plotRepository.findVisiblePlotByIdWithImages(plotId)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found"));

        if (!plot.isVisible()) {
            throw new ResourceNotFoundException("Plot not available in marketplace");
        }

        return plot;
    }

    // =========================
    // 🔍 FILTER PLOTS (CORE)
    // =========================
    public List<Plot> filterPlots(PlotFilterRequest request) {
        Specification<Plot> spec = PlotSpecification.filterPlots(request);
        return plotRepository.findAll(spec);
    }

    // =========================
    // ⚖ COMPARE PLOTS
    // =========================
    public List<Plot> comparePlots(List<Long> plotIds) {
        return plotIds.stream()
                .map(plotRepository::findVisiblePlotByIdWithImages)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(Plot::isVisible)
                .toList();
    }
}