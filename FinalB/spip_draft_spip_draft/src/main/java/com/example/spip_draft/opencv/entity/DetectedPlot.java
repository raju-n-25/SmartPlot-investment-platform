package com.example.spip_draft.opencv.entity;

import com.example.spip_draft.entity.Plot;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores one plot candidate detected by the OpenCV pipeline.
 */
@Entity
@Table(name = "detected_plots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedPlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent processing result */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processing_result_id", nullable = false)
    private LayoutProcessingResult processingResult;

    /** Index of this plot in the processing result (1-based) */
    @Column(name = "plot_index", nullable = false)
    private Integer plotIndex;

    @Column(name = "bbox_x")
    private Integer bboxX;

    @Column(name = "bbox_y")
    private Integer bboxY;

    @Column(name = "bbox_width")
    private Integer bboxWidth;

    @Column(name = "bbox_height")
    private Integer bboxHeight;

    @Column(name = "pixel_area")
    private Double pixelArea;

    @Column(name = "area_sqft")
    private Double areaSqft;

    @Column(name = "area_sqm")
    private Double areaSqm;

    @Column(name = "is_rectangle")
    private Boolean isRectangle;

    /**
     * JSON array of corner points:
     * [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]
     */
    @Column(name = "corner_points_json", length = 2000)
    private String cornerPointsJson;

    /** Optional link to the persisted Plot entity after intelligence step */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_plot_id")
    private Plot linkedPlot;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}