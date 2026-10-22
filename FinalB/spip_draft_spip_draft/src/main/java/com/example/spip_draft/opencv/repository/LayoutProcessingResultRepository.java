package com.example.spip_draft.opencv.repository;

import com.example.spip_draft.opencv.entity.LayoutProcessingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LayoutProcessingResultRepository extends JpaRepository<LayoutProcessingResult, Long> {

    Optional<LayoutProcessingResult> findByLayoutFileId(Long layoutFileId);
}