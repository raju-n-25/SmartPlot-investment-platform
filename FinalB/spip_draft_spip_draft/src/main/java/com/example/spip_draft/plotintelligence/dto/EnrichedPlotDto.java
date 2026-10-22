package com.example.spip_draft.plotintelligence.dto;

import com.example.spip_draft.plotintelligence.enums.FacingDirection;
import com.example.spip_draft.plotintelligence.enums.ShapeType;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrichedPlotDto {

    private Long plotId;
    private Long projectId;

    private Integer contourIndex;
    private Long detectedPlotId;

    private String plotNumber;

    private ShapeType shapeType;

    private Double pixelArea;
    private Double areaSqft;
    private Double areaSqm;

    private String pixelCoordinatesJson;
    private String normalizedCoordinatesJson;

    private FacingDirection facingDirection;
    private String facingLabel;
    private String detectionApproach;

    @Builder.Default
    private List<String> roadFacingEdges = new ArrayList<>();

    private Boolean roadFacing;
    private Boolean cornerPlot;

    private BigDecimal totalPrice;

    private Boolean accepted;
    private String rejectionReason;
}