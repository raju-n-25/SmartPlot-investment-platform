package com.example.spip_draft.plotintelligence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlotIntelligenceRequest {

    @NotNull
    private Long projectId;

    @NotNull
    private Integer imageWidth;

    @NotNull
    private Integer imageHeight;

    private Double scaleFactorPxPerSqft;

    @Builder.Default
    private boolean persist = false;

    @Builder.Default
    private Double boundaryTolerancePx = 10.0;

    @Builder.Default
    private List<List<Double>> roadContourBboxes = new ArrayList<>();

    @Valid
    @NotEmpty
    @Builder.Default
    private List<ContourInputDto> contours = new ArrayList<>();

    public boolean isPersist() {
        return persist;
    }
}