package com.example.spip_draft.opencv.service;

import com.example.spip_draft.opencv.config.OpenCvProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

/**
 * Handles physical storage of uploaded layout files and optional PDF → image conversion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LayoutStorageService {

    private final OpenCvProperties properties;

    /**
     * Saves the uploaded file under:
     *   {uploadDir}/{projectId}/{uuid}.{ext}
     *
     * @return absolute path to the stored file
     */
    public Path storeLayoutFile(Long projectId, MultipartFile file) {
        validateFile(file);

        String ext = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);

        Path projectDir = Paths.get(properties.getUploadDir(), String.valueOf(projectId))
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(projectDir);
            Path target = projectDir.resolve(storedName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("[LayoutStorage] Stored file '{}' → {}", file.getOriginalFilename(), target);
            return target;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store layout file", e);
        }
    }

    /**
     * When the upload is a PDF, render page 0 to PNG and return its path.
     * If the upload is already an image, just return the original path.
     */
    public Path convertPdfToImageIfNeeded(Path originalFilePath, String contentType) {
        if (contentType == null) {
            return originalFilePath;
        }

        boolean isPdf = contentType.equalsIgnoreCase("application/pdf")
                || originalFilePath.getFileName().toString().toLowerCase().endsWith(".pdf");

        if (!isPdf) {
            return originalFilePath;
        }

        Path pngPath = replaceExtension(originalFilePath, "png");

        try (PDDocument document = Loader.loadPDF(originalFilePath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, properties.getPdfRenderDpi());

            ImageIO.write(image, "png", pngPath.toFile());

            log.info("[LayoutStorage] Converted PDF '{}' → PNG '{}'", originalFilePath, pngPath);
            return pngPath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to convert PDF to image", e);
        }
    }

    /**
     * Ensures the debug directory exists for a given project.
     */
    public Path ensureDebugDirectory(Long projectId) {
        Path dir = Paths.get(properties.getDebugDir(), String.valueOf(projectId))
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create debug directory", e);
        }
    }

    /**
     * Writes an intermediate debug image:
     *   {debugDir}/{projectId}/{baseName}-{suffix}.png
     */
    public Path writeDebugImage(Long projectId, String baseName, String suffix, BufferedImage image) {
        Path debugDir = ensureDebugDirectory(projectId);

        String safeBase = (baseName == null || baseName.isBlank())
                ? UUID.randomUUID().toString()
                : baseName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");

        Path out = debugDir.resolve(safeBase + "-" + suffix + ".png");

        try {
            ImageIO.write(image, "png", out.toFile());
            return out;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write debug image: " + suffix, e);
        }
    }

    public String toRelativePath(Path absoluteOrStoredPath) {
        if (absoluteOrStoredPath == null) return null;

        Path p = absoluteOrStoredPath.toAbsolutePath().normalize();
        Path cwd = Paths.get("").toAbsolutePath().normalize();

        try {
            return cwd.relativize(p).toString().replace("\\", "/");
        } catch (Exception e) {
            return p.toString().replace("\\", "/");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Layout file is empty");
        }

        String ct = file.getContentType();
        boolean supported =
                (ct != null && (
                        ct.startsWith("image/")
                                || ct.equalsIgnoreCase("application/pdf")
                ))
                        || hasSupportedExtension(file.getOriginalFilename());

        if (!supported) {
            throw new IllegalArgumentException("Only image files or PDFs are supported for layout upload");
        }
    }

    private boolean hasSupportedExtension(String name) {
        String ext = getExtension(name).toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")
                || ext.equals("webp") || ext.equals("bmp") || ext.equals("pdf");
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) return "";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : "";
    }

    private Path replaceExtension(Path path, String newExt) {
        String fileName = path.getFileName().toString();
        int idx = fileName.lastIndexOf('.');
        String stem = (idx >= 0) ? fileName.substring(0, idx) : fileName;
        return path.getParent().resolve(stem + "." + newExt);
    }
}