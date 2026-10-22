package com.example.spip_draft.opencv.repository;

import com.example.spip_draft.opencv.entity.LayoutFile;
import com.example.spip_draft.opencv.enums.LayoutProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutFileRepository extends JpaRepository<LayoutFile, Long> {

    List<LayoutFile> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<LayoutFile> findByProcessingStatus(LayoutProcessingStatus processingStatus);
}