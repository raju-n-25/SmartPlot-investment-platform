package com.example.spip_draft.dto.project;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectAmenityResponse {

    private Long id;
    private Long projectId;
    private String amenityType;
    private String description;
}