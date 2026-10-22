package com.example.spip_draft.dto.legal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectDocumentResponse {

    private Long id;
    private Long projectId;
    private String documentType;
    private String fileName;
    private String storedFileName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private Boolean verified;
}