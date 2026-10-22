package com.example.spip_draft.opencv.repository;

import com.example.spip_draft.opencv.entity.DetectedPlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetectedPlotRepository extends JpaRepository<DetectedPlot, Long> {

    List<DetectedPlot> findByProcessingResultIdOrderByPlotIndexAsc(Long processingResultId);

    List<DetectedPlot> findByLinkedPlotId(Long linkedPlotId);

    void deleteByProcessingResult_Id(Long processingResultId);
}