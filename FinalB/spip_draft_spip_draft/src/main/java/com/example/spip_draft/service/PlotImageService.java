package com.example.spip_draft.service;

import com.example.spip_draft.dto.marketplace.PlotImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PlotImageService {

    List<PlotImageResponse> uploadImages(Long plotId, List<MultipartFile> files);

    List<PlotImageResponse> getImages(Long plotId);

    void deleteImage(Long imageId);
}