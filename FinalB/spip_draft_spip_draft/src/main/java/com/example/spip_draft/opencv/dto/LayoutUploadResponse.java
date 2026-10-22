package com.example.spip_draft.opencv.dto;

import com.example.spip_draft.opencv.enums.LayoutProcessingStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Returned immediately after a layout file is uploaded (before processing).
 */
@Data
@Builder
public class LayoutUploadResponse {
    private Long layoutFileId;
    private Long projectId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private LayoutProcessingStatus processingStatus;
    private String message;
}