package com.example.spip_draft.dto.marketplace;

import lombok.Builder;
import java.util.List;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlotResponse {

    private Long id;
    private String plotNumber;
    private Double area;
    private BigDecimal price;
    private String facing;
    private Boolean cornerPlot;

    private String status;
    private Boolean available;

    private String projectName;
    private String builderName;
    private Double builderRating;

    private String khataType;
    private String khataRemarks;

    private Integer plotRiskScore;
    private String plotRiskCategory;
    private String bookingStatus;
    
    private String plotDescription;
    private String plotDimensions;
    private Double roadWidth;
    private String mapLink;
    private String primaryImageUrl;
    private List<PlotImageResponse> plotImages;
}