package com.example.spip_draft.repository;

import com.example.spip_draft.entity.PlotImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlotImageRepository extends JpaRepository<PlotImage, Long> {

    List<PlotImage> findByPlot_Id(Long plotId);
}