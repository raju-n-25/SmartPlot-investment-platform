package com.example.spip_draft.plotintelligence.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlotIntelligenceResponse {

    private Long projectId;

    private Integer totalContoursReceived;
    private Integer accepted;
    private Integer rejected;
    private Integer persisted;
    private Boolean dryRun;
    private Double totalAreaSqft;
    private Integer roadFacingCount;
    private Integer cornerPlotCount;

    @Builder.Default
    private List<EnrichedPlotDto> plots = new ArrayList<>();
}