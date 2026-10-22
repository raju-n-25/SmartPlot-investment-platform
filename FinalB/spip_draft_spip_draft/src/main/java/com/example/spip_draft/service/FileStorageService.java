package com.example.spip_draft.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final String PROFILE_PHOTO_DIR = "uploads/profile-photos";
    private static final String BUILDER_DOCUMENT_DIR = "uploads/builder-docs";
    private static final String INVESTOR_DOCUMENT_DIR = "uploads/investor-docs";
    private static final String AGREEMENT_DIR = "uploads/agreements";

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png"
    );

    public String storeProfilePhoto(MultipartFile file) {
        try {
            validateProfilePhoto(file);

            Path uploadPath = Paths.get(PROFILE_PHOTO_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFileName = file.getOriginalFilename();
            String extension = getFileExtension(originalFileName);

            String storedFileName = UUID.randomUUID() + extension;
            Path targetLocation = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/profile-photos/" + storedFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Failed to store profile photo: " + ex.getMessage(), ex);
        }
    }

    public String storeBuilderDocument(MultipartFile file) {
        try {
            validateDocumentFile(file);

            Path uploadPath = Paths.get(BUILDER_DOCUMENT_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFileName = file.getOriginalFilename();
            String extension = getFileExtension(originalFileName);

            String storedFileName = UUID.randomUUID() + extension;
            Path targetLocation = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/builder-docs/" + storedFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Failed to store builder document: " + ex.getMessage(), ex);
        }
    }

    public String storeInvestorDocument(MultipartFile file) {
        try {
            validateDocumentFile(file);

            Path uploadPath = Paths.get(INVESTOR_DOCUMENT_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFileName = file.getOriginalFilename();
            String extension = getFileExtension(originalFileName);

            String storedFileName = UUID.randomUUID() + extension;
            Path targetLocation = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/investor-docs/" + storedFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Failed to store investor document: " + ex.getMessage(), ex);
        }
    }

    private void validateProfilePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();

        boolean validExtension = IMAGE_EXTENSIONS.contains(extension);
        boolean validContentType = contentType.startsWith("image/");

        if (!validExtension || !validContentType) {
            throw new RuntimeException("Only JPG, JPEG, PNG, or WEBP image files are allowed");
        }

        validateFileSize(file, 5);
    }

    private void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Document file is empty");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();

        boolean validExtension = DOCUMENT_EXTENSIONS.contains(extension);
        boolean validContentType =
                "application/pdf".equals(contentType) ||
                "image/jpeg".equals(contentType) ||
                "image/jpg".equals(contentType) ||
                "image/png".equals(contentType);

        if (!validExtension || !validContentType) {
            throw new RuntimeException("Allowed formats: PDF, JPG, JPEG, PNG");
        }

        validateFileSize(file, 10);
    }

    private void validateFileSize(MultipartFile file, int maxSizeMb) {
        long maxSizeBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException("File size must be under " + maxSizeMb + " MB");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }
    public String storePlotImage(MultipartFile file) {
        return storeProfilePhoto(file); // reuse logic for now
    }

    public String storeAgreementPdf(byte[] pdfBytes, String fileName) {
        try {
            Path uploadPath = Paths.get(AGREEMENT_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String safeFileName = fileName.endsWith(".pdf") ? fileName : fileName + ".pdf";
            Path targetLocation = uploadPath.resolve(safeFileName);

            Files.write(
                    targetLocation,
                    pdfBytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            return "/uploads/agreements/" + safeFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Failed to store agreement PDF: " + ex.getMessage(), ex);
        }
    }
}