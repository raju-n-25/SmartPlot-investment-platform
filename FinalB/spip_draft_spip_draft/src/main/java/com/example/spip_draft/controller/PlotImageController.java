package com.example.spip_draft.controller;

import com.example.spip_draft.dto.marketplace.PlotImageResponse;
import com.example.spip_draft.service.PlotImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/builder/plots")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlotImageController {

    private final PlotImageService plotImageService;

    // Upload multiple images
    @PostMapping("/{plotId}/images")
    public ResponseEntity<List<PlotImageResponse>> uploadImages(
            @PathVariable Long plotId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(plotImageService.uploadImages(plotId, files));
    }

    // Get images
    @GetMapping("/{plotId}/images")
    public ResponseEntity<List<PlotImageResponse>> getImages(@PathVariable Long plotId) {
        return ResponseEntity.ok(plotImageService.getImages(plotId));
    }

    // Delete image
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<String> deleteImage(@PathVariable Long imageId) {
        plotImageService.deleteImage(imageId);
        return ResponseEntity.ok("Image deleted successfully");
    }
}