package com.example.spip_draft.dto.project;

import lombok.Data;
import java.util.List;

import java.time.LocalDateTime;

@Data
public class ProjectResponse {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long tenantId;

    private Long builderId;
    private String builderCompanyName;

    private String projectName;
    private String description;
    private String location;
    private String city;
    private String state;

    private Integer totalPlots;
    private Integer availablePlots;
    private List<String> amenities;
    private String status;
}