package com.example.spip_draft.dto.document;

import com.example.spip_draft.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDocumentUploadRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private String originalFileName;

    // Required only for resubmission / replacement cases
    private String resubmissionReason;
}
