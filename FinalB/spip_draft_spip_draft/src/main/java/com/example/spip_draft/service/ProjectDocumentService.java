package com.example.spip_draft.service;

import com.example.spip_draft.dto.legal.ProjectDocumentResponse;
import com.example.spip_draft.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectDocumentService {

    ProjectDocumentResponse uploadDocument(Long projectId, DocumentType documentType, MultipartFile file);

    List<ProjectDocumentResponse> getProjectDocuments(Long projectId);
}