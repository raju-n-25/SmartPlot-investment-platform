package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.marketplace.PlotImageResponse;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.entity.PlotImage;
import com.example.spip_draft.multitenant.TenantContext;
import com.example.spip_draft.repository.PlotImageRepository;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.service.FileStorageService;
import com.example.spip_draft.service.PlotImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlotImageServiceImpl implements PlotImageService {

    private final PlotRepository plotRepository;
    private final PlotImageRepository plotImageRepository;
    private final FileStorageService fileStorageService;

    @Override
    public List<PlotImageResponse> uploadImages(Long plotId, List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw new RuntimeException("At least one image is required");
        }

        Long tenantId = getTenantId();

        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new RuntimeException("Plot not found"));

        validatePlotOwnership(plot, tenantId);

        int existingCount = plotImageRepository.findByPlot_Id(plotId).size();
        boolean hasPrimary = plot.getPrimaryImageUrl() != null && !plot.getPrimaryImageUrl().isBlank();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            String imageUrl = fileStorageService.storePlotImage(file);

            PlotImage image = new PlotImage();
            image.setPlot(plot);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(existingCount + i);

            boolean makePrimary = !hasPrimary && i == 0;
            image.setIsPrimary(makePrimary);

            plotImageRepository.save(image);

            if (makePrimary) {
                plot.setPrimaryImageUrl(imageUrl);
                hasPrimary = true;
            }
        }

        plotRepository.save(plot);

        return plotImageRepository.findByPlot_Id(plotId)
                .stream()
                .sorted(Comparator.comparing(img -> img.getDisplayOrder() == null ? Integer.MAX_VALUE : img.getDisplayOrder()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlotImageResponse> getImages(Long plotId) {
        Long tenantId = getTenantId();

        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new RuntimeException("Plot not found"));

        validatePlotOwnership(plot, tenantId);

        return plotImageRepository.findByPlot_Id(plotId)
                .stream()
                .sorted(Comparator.comparing(img -> img.getDisplayOrder() == null ? Integer.MAX_VALUE : img.getDisplayOrder()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteImage(Long imageId) {
        Long tenantId = getTenantId();

        PlotImage image = plotImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        Plot plot = image.getPlot();
        if (plot == null) {
            throw new RuntimeException("Plot not found for image");
        }

        validatePlotOwnership(plot, tenantId);

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        String deletingUrl = image.getImageUrl();

        plotImageRepository.delete(image);

        List<PlotImage> remaining = plotImageRepository.findByPlot_Id(plot.getId())
                .stream()
                .sorted(Comparator.comparing(img -> img.getDisplayOrder() == null ? Integer.MAX_VALUE : img.getDisplayOrder()))
                .toList();

        if (remaining.isEmpty()) {
            if (deletingUrl != null && deletingUrl.equals(plot.getPrimaryImageUrl())) {
                plot.setPrimaryImageUrl(null);
            }
            plotRepository.save(plot);
            return;
        }

        if (wasPrimary || deletingUrl != null && deletingUrl.equals(plot.getPrimaryImageUrl())) {
            PlotImage newPrimary = remaining.get(0);

            for (int i = 0; i < remaining.size(); i++) {
                PlotImage img = remaining.get(i);
                img.setDisplayOrder(i);
                img.setIsPrimary(i == 0);
                plotImageRepository.save(img);
            }

            plot.setPrimaryImageUrl(newPrimary.getImageUrl());
            plotRepository.save(plot);
        } else {
            for (int i = 0; i < remaining.size(); i++) {
                PlotImage img = remaining.get(i);
                img.setDisplayOrder(i);
                plotImageRepository.save(img);
            }
        }
    }

    private void validatePlotOwnership(Plot plot, Long tenantId) {
        if (plot.getTenantId() == null || !plot.getTenantId().equals(tenantId)) {
            throw new RuntimeException("You do not have access to this plot");
        }
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context is missing");
        }
        return tenantId;
    }

    private PlotImageResponse mapToResponse(PlotImage image) {
        return PlotImageResponse.builder()
                .id(image.getId())
                .plotId(image.getPlot() != null ? image.getPlot().getId() : null)
                .imageUrl(image.getImageUrl())
                .isPrimary(Boolean.TRUE.equals(image.getIsPrimary()))
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}