package com.example.spip_draft.dto.project;

import lombok.Data;

@Data
public class ProjectCreateResponse {
    private ProjectResponse project;
    private String message;
    private String kycStatus;
}