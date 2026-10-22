package com.example.spip_draft.dto.marketplace;

import jakarta.validation.constraints.*;
import com.example.spip_draft.enums.KathaType;
import lombok.Data;

@Data
public class PlotCreateRequest {

    @NotBlank(message = "Plot number is required")
    @Size(max = 50, message = "Plot number must be less than 50 characters")
    private String plotNumber;

    @NotNull
    @Positive(message = "Area must be greater than 0")
    private Double area;

    @NotNull
    @Positive(message = "Price must be greater than 0")
    private Double price;

    // ✅ FIXED (case-insensitive)
    @Pattern(
            regexp = "^(?i)(north|south|east|west|north-east|north-west|south-east|south-west)?$",
            message = "Invalid facing direction"
    )
    private String facing;

    private Boolean cornerPlot = false;

    @Size(max = 1000)
    private String legalInfo;
    
    private Boolean available;

    @Min(0)
    @Max(100)
    private Double riskScore;
    private KathaType khataType;
    private String khataRemarks;
    
    @Size(max = 2000)
    private String plotDescription;

    @Size(max = 100)
    private String plotDimensions;

    @PositiveOrZero(message = "Road width must be 0 or greater")
    private Double roadWidth;

    @Size(max = 1000)
    private String mapLink;

}

