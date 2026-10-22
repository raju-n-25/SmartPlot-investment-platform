package com.example.spip_draft.opencv.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the Module 7 layout processing pipeline.
 * Prefix: layout.opencv
 *
 * v4 additions:
 *   - adaptiveRadius       (Mode A – Aerial local-adaptive threshold)
 *   - blueprintDarkThresh  (Mode B – Blueprint dark-line threshold)
 *
 * Mode is chosen automatically by measuring mean image brightness:
 *   mean > 180 → Blueprint mode
 *   mean ≤ 180 → Aerial mode
 */
@Component
@ConfigurationProperties(prefix = "layout.opencv")
@Getter
@Setter
public class OpenCvProperties {

    private String uploadDir = "uploads/layouts";
    private String debugDir  = "uploads/layouts/debug";

    /**
     * Gaussian blur neighbourhood radius for the local-adaptive threshold.
     * sigma = adaptiveRadius / 4.0  →  default 40 / 4 = σ 10.
     * Covers ~40 px on each side of a pixel.
     */
    private int adaptiveRadius = 40;

    /**
     * Local brightness offset for Mode A.
     * A pixel is flagged as a grid-line barrier when:
     *   gray[y][x] - localMean[y][x]  >  cannyThreshold1
     * Default 10.0 works well for JPEG aerial layout renders.
     */
    private double cannyThreshold1 = 10.0;

    /** Kept for interface compatibility (not used in v4). */
    private double cannyThreshold2 = 150.0;

    /** Kept for interface compatibility (not used in v4). */
    private int gaussianKernel = 5;

    /**
     * Gray-level threshold for blueprint/plan images.
     * Pixels with gray < blueprintDarkThresh are classified as border lines.
     * Everything at or above this value is plot interior (white).
     *
     * Default 140 works for standard black-on-white layout plans.
     * Lower (e.g. 120) if thin lines are being missed.
     * Higher (e.g. 160) if text inside plots is breaking cells apart.
     */
    private int blueprintDarkThresh = 140;

    /**
     * Minimum pixel count for a connected interior region to be kept as a plot.
     * Removes noise, stray labels, and road slivers.
     * Default 500 px.
     */
    private double minPlotAreaPx = 500.0;

    /**
     * Maximum plot size as a fraction of total image area.
     * Regions larger than this are outer boundary, roads, or background.
     * Default 0.05 (5 %) covers layouts with as few as 20 plots.
     * Reduce to 0.025 for dense layouts with 100+ plots.
     */
    private double maxPlotAreaRatio = 0.05;

    /** Kept for interface compatibility (not used in v4). */
    private double contourApproxEpsilonRatio = 0.02;

    /**
     * Minimum fill ratio: pixelCount / (bboxWidth × bboxHeight).
     * 1.0 = perfectly solid rectangle.
     * Default 0.50 accepts slightly irregular or JPEG-degraded cells.
     */
    private double minRectangularity = 0.50;

    /**
     * Maximum aspect ratio (longSide / shortSide).
     * Default 4.5 accepts narrow plots while rejecting road-shaped regions.
     */
    private double maxAspectRatio = 4.5;

    private double defaultScaleFactorPxPerSqft = 100.0;

    private int pdfRenderDpi = 150;
}