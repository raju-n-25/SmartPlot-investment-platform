package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.legal.ProjectDocumentResponse;
import com.example.spip_draft.entity.Project;
import com.example.spip_draft.entity.ProjectDocument;
import com.example.spip_draft.enums.DocumentType;
import com.example.spip_draft.repository.ProjectDocumentRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.service.ProjectDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectDocumentServiceImpl implements ProjectDocumentService {

    private final ProjectRepository projectRepository;
    private final ProjectDocumentRepository projectDocumentRepository;

    private static final String UPLOAD_DIR = "uploads/project-docs";

    @Override
    public ProjectDocumentResponse uploadDocument(Long projectId, DocumentType documentType, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "document");

        String extension = "";

        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFileName.substring(dotIndex);
        }

        String storedFileName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath);

            ProjectDocument projectDocument = new ProjectDocument();
            projectDocument.setProject(project);
            projectDocument.setDocumentType(documentType);
            projectDocument.setFileName(originalFileName);
            projectDocument.setStoredFileName(storedFileName);
            projectDocument.setFilePath(targetPath.toString());
            projectDocument.setContentType(file.getContentType());
            projectDocument.setFileSize(file.getSize());
            projectDocument.setVerified(false);

            ProjectDocument saved = projectDocumentRepository.save(projectDocument);

            return mapToResponse(saved);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProjectDocumentResponse> getProjectDocuments(Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Project not found");
        }

        return projectDocumentRepository.findByProject_Id(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProjectDocumentResponse mapToResponse(ProjectDocument document) {
        return ProjectDocumentResponse.builder()
                .id(document.getId())
                .projectId(document.getProject().getId())
                .documentType(document.getDocumentType() != null ? document.getDocumentType().name() : null)
                .fileName(document.getFileName())
                .storedFileName(document.getStoredFileName())
                .filePath(document.getFilePath())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .verified(document.getVerified())
                .build();
    }
}