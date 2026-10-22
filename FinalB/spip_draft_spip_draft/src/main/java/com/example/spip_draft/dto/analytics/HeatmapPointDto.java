package com.example.spip_draft.dto.analytics;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class HeatmapPointDto {

    private Long plotId;
    private String plotNumber;
    private String projectName;

    private String facing;
    private Boolean cornerPlot;

    private BigDecimal pricePerSqft;
    private BigDecimal demandScore;
    private BigDecimal infrastructureScore;

    private Integer riskScore;
    private String recommendation;
}