package com.example.spip_draft.opencv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores one completed OpenCV processing run for a layout file.
 */
@Entity
@Table(name = "layout_processing_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutProcessingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One processing result per uploaded layout file */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_file_id", nullable = false, unique = true)
    private LayoutFile layoutFile;

    @Column(name = "total_plots_detected")
    private Integer totalPlotsDetected;

    @Column(name = "scale_factor_px_per_sqft")
    private Double scaleFactorPxPerSqft;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "debug_grayscale_path", length = 1000)
    private String debugGrayscalePath;

    @Column(name = "debug_edge_path", length = 1000)
    private String debugEdgePath;

    @Column(name = "debug_contour_path", length = 1000)
    private String debugContourPath;

    @Column(name = "debug_filtered_plots_path", length = 1000)
    private String debugFilteredPlotsPath;

    @OneToMany(mappedBy = "processingResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetectedPlot> detectedPlots = new ArrayList<>();

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