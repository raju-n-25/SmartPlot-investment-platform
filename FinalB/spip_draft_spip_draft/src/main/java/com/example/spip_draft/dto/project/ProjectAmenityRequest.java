package com.example.spip_draft.dto.project;

import com.example.spip_draft.enums.AmenityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectAmenityRequest {

    @NotNull(message = "Amenity type is required")
    private AmenityType amenityType;

    private String description;
}