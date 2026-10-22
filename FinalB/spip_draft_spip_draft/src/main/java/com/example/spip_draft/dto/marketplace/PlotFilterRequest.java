package com.example.spip_draft.dto.marketplace;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlotFilterRequest {

    private Double minArea;
    private Double maxArea;

    private Double minPrice;
    private Double maxPrice;

    private String facing;
    private Boolean cornerPlot;
    private Long projectId;

    // 🔥 NEW (pagination)
    private int page = 0;
    private int size = 10;

    // 🔥 NEW (sorting)
    private String sortBy = "price"; // price, area, createdAt
    private String sortDirection = "ASC"; // ASC / DESC
}