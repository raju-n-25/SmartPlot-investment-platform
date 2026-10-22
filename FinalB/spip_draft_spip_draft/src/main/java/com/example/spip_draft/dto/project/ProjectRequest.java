package com.example.spip_draft.dto.project;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String projectName;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotNull(message = "Total plots is required")
    @Min(value = 1, message = "Total plots must be greater than 0")
    private Integer totalPlots;

    @NotNull(message = "Available plots is required")
    @Min(value = 0, message = "Available plots cannot be negative")
    private Integer availablePlots;
}