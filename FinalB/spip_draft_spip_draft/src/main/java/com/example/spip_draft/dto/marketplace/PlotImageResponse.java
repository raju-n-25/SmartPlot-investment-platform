package com.example.spip_draft.dto.marketplace;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlotImageResponse {

    private Long id;
    private Long plotId;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
}