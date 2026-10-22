package com.example.spip_draft.mapper;

import com.example.spip_draft.dto.marketplace.PlotImageResponse;
import com.example.spip_draft.dto.marketplace.PlotResponse;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.entity.PlotImage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlotMapper {

    public static PlotResponse toResponse(Plot plot) {
        List<PlotImageResponse> imageResponses =
                plot.getImages() == null
                        ? List.of()
                        : plot.getImages().stream()
                        .sorted(Comparator.comparing(
                                PlotImage::getDisplayOrder,
                                Comparator.nullsLast(Integer::compareTo)
                        ))
                        .map(PlotMapper::toImageResponse)
                        .collect(Collectors.toList());

        return PlotResponse.builder()
                .id(plot.getId())
                .plotNumber(plot.getPlotNumber())
                .area(plot.getArea())
                .price(plot.getPrice())
                .facing(plot.getFacing())
                .cornerPlot(plot.getCornerPlot())
                .status(plot.getStatus() != null ? plot.getStatus().name() : null)
                .available(plot.getAvailable())
                .projectName(plot.getProject() != null ? plot.getProject().getProjectName() : null)
                .builderName(
                        plot.getProject() != null && plot.getProject().getBuilder() != null
                                ? plot.getProject().getBuilder().getCompanyName()
                                : null
                )
                .builderRating(
                        plot.getProject() != null && plot.getProject().getBuilder() != null
                                ? plot.getProject().getBuilder().getRating()
                                : null
                )
                .khataType(plot.getKhataType() != null ? plot.getKhataType().name() : null)
                .khataRemarks(plot.getKhataRemarks())
                .plotDescription(plot.getPlotDescription())
                .plotDimensions(plot.getPlotDimensions())
                .roadWidth(plot.getRoadWidth())
                .mapLink(plot.getMapLink())
                .primaryImageUrl(plot.getPrimaryImageUrl())
                .plotImages(imageResponses)
                .plotRiskScore(plot.getPlotRiskScore())
                .plotRiskCategory(
                        plot.getPlotRiskCategory() != null
                                ? plot.getPlotRiskCategory().name()
                                : null
                )
                .bookingStatus(plot.getBookingStatus() != null ? plot.getBookingStatus().name() : null)
                .build();
    }

    private static PlotImageResponse toImageResponse(PlotImage image) {
        return PlotImageResponse.builder()
                .id(image.getId())
                .plotId(image.getPlot() != null ? image.getPlot().getId() : null)
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}