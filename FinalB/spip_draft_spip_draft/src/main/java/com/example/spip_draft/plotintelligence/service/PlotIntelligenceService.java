package com.example.spip_draft.plotintelligence.service;

import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.entity.Project;
import com.example.spip_draft.enums.PlotApprovalStatus;
import com.example.spip_draft.opencv.entity.DetectedPlot;
import com.example.spip_draft.opencv.repository.DetectedPlotRepository;
import com.example.spip_draft.plotintelligence.dto.*;
import com.example.spip_draft.plotintelligence.enums.FacingDirection;
import com.example.spip_draft.plotintelligence.enums.ShapeType;
import com.example.spip_draft.plotintelligence.util.*;
import com.example.spip_draft.repository.PlotRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║       Plot Intelligence Service  –  Module 7, Submodule 2                  ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║  Converts raw OpenCV contour output into enriched, structured Plot entities.║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlotIntelligenceService {

    private static final double DEFAULT_SCALE_PX_PER_SQFT = 100.0;

    private final ProjectRepository projectRepository;
    private final PlotRepository plotRepository;
    private final DetectedPlotRepository detectedPlotRepository;

    // =========================================================================
    // Access helpers
    // =========================================================================

    private Project getAccessibleProject(Long projectId) {
        Long builderId = SecurityUtil.getCurrentBuilderId();

        if (builderId == null) {
            throw new RuntimeException("Only builder users can access plot intelligence.");
        }

        return projectRepository.findByIdAndBuilderId(projectId, builderId)
                .orElseThrow(() -> new RuntimeException(
                        "Project not found or you do not have access to this project: id=" + projectId));
    }

    private String resolveUniquePlotNumber(Long projectId, String requestedPlotNumber) {
        String base = (requestedPlotNumber == null || requestedPlotNumber.isBlank())
                ? "PLOT"
                : requestedPlotNumber.trim();

        String candidate = base;
        int suffix = 1;

        while (plotRepository.existsByProject_IdAndPlotNumberIgnoreCase(projectId, candidate)) {
            candidate = base + "-" + suffix++;
        }

        return candidate;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    @Transactional
    public PlotIntelligenceResponse processContours(PlotIntelligenceRequest request) {

        log.info("[PlotIntelligence] Processing {} contours for project={}",
                request.getContours().size(), request.getProjectId());

        Project project = getAccessibleProject(request.getProjectId());

        double scale = (request.getScaleFactorPxPerSqft() != null
                && request.getScaleFactorPxPerSqft() > 0)
                ? request.getScaleFactorPxPerSqft()
                : DEFAULT_SCALE_PX_PER_SQFT;

        List<double[]> roadBboxes = parseRoadBboxes(request.getRoadContourBboxes());
        List<double[]> allPlotBboxes = computeAllPlotBboxes(request.getContours());

        List<EnrichedPlotDto> results = new ArrayList<>();
        int persistedCount = 0;
        int contourIdx = 1;

        for (ContourInputDto contour : request.getContours()) {
            EnrichedPlotDto enriched = processSingleContour(
                    contour,
                    contourIdx++,
                    project,
                    scale,
                    request.getImageWidth(),
                    request.getImageHeight(),
                    roadBboxes,
                    allPlotBboxes,
                    request.getBoundaryTolerancePx(),
                    request.isPersist()
            );

            results.add(enriched);
            if (Boolean.TRUE.equals(enriched.getAccepted()) && enriched.getPlotId() != null) {
                persistedCount++;
            }
        }

        int accepted = (int) results.stream().filter(r -> Boolean.TRUE.equals(r.getAccepted())).count();
        int rejected = results.size() - accepted;
        double totalSqft = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getAccepted()))
                .mapToDouble(r -> r.getAreaSqft() != null ? r.getAreaSqft() : 0)
                .sum();
        int roadFacing = (int) results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getRoadFacing()))
                .count();
        int cornerPlots = (int) results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getCornerPlot()))
                .count();

        log.info("[PlotIntelligence] Done: accepted={}, rejected={}, persisted={}, roadFacing={}, corners={}",
                accepted, rejected, persistedCount, roadFacing, cornerPlots);

        return PlotIntelligenceResponse.builder()
                .projectId(project.getId())
                .totalContoursReceived(request.getContours().size())
                .accepted(accepted)
                .rejected(rejected)
                .persisted(persistedCount)
                .dryRun(!request.isPersist())
                .plots(results)
                .totalAreaSqft(GeometryUtils.round2(totalSqft))
                .roadFacingCount(roadFacing)
                .cornerPlotCount(cornerPlots)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EnrichedPlotDto> getEnrichedPlotsForProject(Long projectId) {
        Project project = getAccessibleProject(projectId);

        return plotRepository.findByProjectId(project.getId()).stream()
                .map(this::toEnrichedDto)
                .toList();
    }

    // =========================================================================
    // Single-contour processing pipeline
    // =========================================================================

    private EnrichedPlotDto processSingleContour(ContourInputDto contour,
                                                 int contourIdx,
                                                 Project project,
                                                 double scale,
                                                 double imgWidth,
                                                 double imgHeight,
                                                 List<double[]> roadBboxes,
                                                 List<double[]> allPlotBboxes,
                                                 double boundaryTol,
                                                 boolean persist) {

        EnrichedPlotDto.EnrichedPlotDtoBuilder builder = EnrichedPlotDto.builder()
                .contourIndex(contourIdx)
                .projectId(project.getId())
                .detectedPlotId(contour.getDetectedPlotId());

        List<double[]> points;
        try {
            points = resolvePoints(contour);
        } catch (Exception e) {
            log.warn("[PlotIntelligence] Contour {} parse failed: {}", contourIdx, e.getMessage());
            return builder.accepted(false).rejectionReason("Parse error: " + e.getMessage()).build();
        }

        if (!ContourParser.isValid(points)) {
            return builder
                    .accepted(false)
                    .rejectionReason("Invalid contour: fewer than 4 points (" + points.size() + ")")
                    .build();
        }

        ShapeType shapeType = ContourParser.classify(points);
        builder.shapeType(shapeType);

        double[] bbox = GeometryUtils.boundingBox(points);
        double pixelArea;
        if (shapeType == ShapeType.RECTANGLE) {
            pixelArea = GeometryUtils.rectangleArea(bbox[2] - bbox[0], bbox[3] - bbox[1]);
        } else {
            pixelArea = GeometryUtils.shoelaceArea(points);
        }
        builder.pixelArea(GeometryUtils.round2(pixelArea));

        double areaSqft = GeometryUtils.toSqft(pixelArea, scale);
        double areaSqm = GeometryUtils.sqftToSqm(areaSqft);
        builder.areaSqft(areaSqft).areaSqm(areaSqm);

        String pixelJson = resolvePixelJson(contour, points);
        builder.pixelCoordinatesJson(pixelJson);

        List<double[]> normalised = GeometryUtils.normalise(points, imgWidth, imgHeight);
        String normJson = ContourParser.toJson(normalised);
        builder.normalizedCoordinatesJson(normJson);

        List<double[]> otherPlotBboxes = allPlotBboxes.stream()
                .filter(b -> !bboxEquals(b, bbox))
                .toList();

        FacingDirection facing = RoadDetector.detect(
                bbox,
                otherPlotBboxes,
                imgWidth,
                imgHeight,
                roadBboxes,
                contour.getManualFacingDirection(),
                boundaryTol,
                boundaryTol,
                DirectionUtils.SHARED_EDGE_TOLERANCE_PX
        );

        String detectionApproach = facing != FacingDirection.UNKNOWN ? "COMBINED" : "UNKNOWN";

        if (facing == FacingDirection.UNKNOWN) {
            if (contour.getManualFacingDirection() != null && !contour.getManualFacingDirection().isBlank()) {
                facing = DirectionUtils.fromUserInput(contour.getManualFacingDirection());
                if (facing != FacingDirection.UNKNOWN) {
                    detectionApproach = "USER_INPUT";
                }
            }
        }

        boolean roadFacing = DirectionUtils.isRoadFacing(facing);
        boolean cornerPlot = DirectionUtils.isCornerPlot(facing);
        List<String> roadFacingEdges = DirectionUtils.roadFacingEdges(facing);
        String facingLabel = DirectionUtils.facingLabel(facing);

        builder.facingDirection(facing)
                .facingLabel(facingLabel)
                .detectionApproach(detectionApproach)
                .roadFacingEdges(roadFacingEdges)
                .roadFacing(roadFacing)
                .cornerPlot(cornerPlot);

        String requestedPlotNumber = (contour.getPlotNumber() != null && !contour.getPlotNumber().isBlank())
                ? contour.getPlotNumber()
                : "PLOT-" + contourIdx;

        String plotNumber = persist
                ? resolveUniquePlotNumber(project.getId(), requestedPlotNumber)
                : requestedPlotNumber;

        builder.plotNumber(plotNumber);

        BigDecimal pricePerSqft = contour.getPricePerSqft();
        BigDecimal totalPrice = null;
        if (pricePerSqft != null && pricePerSqft.compareTo(BigDecimal.ZERO) > 0) {
            totalPrice = pricePerSqft.multiply(BigDecimal.valueOf(areaSqft));
        }
        builder.totalPrice(totalPrice);

        if (persist) {
            Plot plot = buildPlotEntity(
                    project,
                    plotNumber,
                    normJson,
                    shapeType,
                    areaSqft,
                    facing,
                    roadFacing,
                    cornerPlot,
                    totalPrice != null ? totalPrice : BigDecimal.ZERO
            );

            plot = plotRepository.save(plot);

            project.setTotalPlots((project.getTotalPlots() == null ? 0 : project.getTotalPlots()) + 1);

            if (contour.getDetectedPlotId() != null) {
                linkDetectedPlot(contour.getDetectedPlotId(), plot.getId());
            }

            builder.plotId(plot.getId());
            log.debug("[PlotIntelligence] Persisted Plot id={} ({})", plot.getId(), plotNumber);
        }

        builder.accepted(true);
        return builder.build();
    }

    // =========================================================================
    // Entity builder
    // =========================================================================

    private Plot buildPlotEntity(Project project,
                                 String plotNumber,
                                 String normalizedCoordinatesJson,
                                 ShapeType shapeType,
                                 double areaSqft,
                                 FacingDirection facing,
                                 boolean roadFacing,
                                 boolean cornerPlot,
                                 BigDecimal price) {

        Plot plot = new Plot();
        plot.setProject(project);
        plot.setTenantId(project.getTenantId());
        plot.setPlotNumber(plotNumber);
        plot.setCoordinates(normalizedCoordinatesJson);
        plot.setShapeType(shapeType.name());
        plot.setArea(areaSqft);
        plot.setFacing(facing.name());
        plot.setRoadFacing(roadFacing);
        plot.setCornerPlot(cornerPlot);
        plot.setPrice(price);
        plot.setStatus(PlotApprovalStatus.PENDING);
        plot.setAvailable(false);

        return plot;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<double[]> computeAllPlotBboxes(List<ContourInputDto> contours) {
        List<double[]> bboxes = new ArrayList<>();
        for (ContourInputDto contour : contours) {
            try {
                List<double[]> pts = resolvePoints(contour);
                if (ContourParser.isValid(pts)) {
                    bboxes.add(GeometryUtils.boundingBox(pts));
                }
            } catch (Exception ignored) {
            }
        }
        return bboxes;
    }

    private List<double[]> resolvePoints(ContourInputDto contour) {
        if (contour.getPoints() != null && !contour.getPoints().isEmpty()) {
            return contour.getPoints().stream()
                    .map(ContourPoint::toArray)
                    .toList();
        }
        if (contour.getCornerPointsJson() != null && !contour.getCornerPointsJson().isBlank()) {
            return ContourParser.parse(contour.getCornerPointsJson());
        }
        throw new IllegalArgumentException("Neither 'points' nor 'cornerPointsJson' was provided");
    }

    private String resolvePixelJson(ContourInputDto contour, List<double[]> parsedPoints) {
        if (contour.getCornerPointsJson() != null && !contour.getCornerPointsJson().isBlank()) {
            return contour.getCornerPointsJson();
        }
        return ContourParser.toJson(parsedPoints);
    }

    private List<double[]> parseRoadBboxes(List<List<Double>> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .filter(r -> r != null && r.size() == 4)
                .map(r -> new double[]{r.get(0), r.get(1), r.get(2), r.get(3)})
                .toList();
    }

    private void linkDetectedPlot(Long detectedPlotId, Long plotId) {
        Optional<DetectedPlot> opt = detectedPlotRepository.findById(detectedPlotId);
        opt.ifPresentOrElse(dp -> {
            Plot linkedPlot = plotRepository.findById(plotId)
                    .orElseThrow(() -> new RuntimeException("Plot not found: id=" + plotId));
            dp.setLinkedPlot(linkedPlot);
            detectedPlotRepository.save(dp);
            log.debug("[PlotIntelligence] Linked DetectedPlot id={} → Plot id={}", detectedPlotId, plotId);
        }, () -> log.warn("[PlotIntelligence] DetectedPlot id={} not found — skipping link", detectedPlotId));
    }

    private EnrichedPlotDto toEnrichedDto(Plot plot) {
        FacingDirection facing = parseFacing(plot.getFacing());
        ShapeType shape = parseShape(plot.getShapeType());

        return EnrichedPlotDto.builder()
                .plotId(plot.getId())
                .projectId(plot.getProject().getId())
                .plotNumber(plot.getPlotNumber())
                .normalizedCoordinatesJson(plot.getCoordinates())
                .shapeType(shape)
                .areaSqft(plot.getArea())
                .areaSqm(GeometryUtils.sqftToSqm(plot.getArea() != null ? plot.getArea() : 0))
                .facingDirection(facing)
                .facingLabel(DirectionUtils.facingLabel(facing))
                .roadFacingEdges(DirectionUtils.roadFacingEdges(facing))
                .roadFacing(plot.getRoadFacing())
                .cornerPlot(plot.getCornerPlot())
                .totalPrice(plot.getPrice())
                .accepted(true)
                .build();
    }

    private FacingDirection parseFacing(String value) {
        if (value == null) return FacingDirection.UNKNOWN;
        try {
            return FacingDirection.valueOf(value);
        } catch (IllegalArgumentException e) {
            return FacingDirection.UNKNOWN;
        }
    }

    private ShapeType parseShape(String value) {
        if (value == null) return ShapeType.RECTANGLE;
        try {
            return ShapeType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ShapeType.RECTANGLE;
        }
    }

    private static boolean bboxEquals(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < 1.0
                && Math.abs(a[1] - b[1]) < 1.0
                && Math.abs(a[2] - b[2]) < 1.0
                && Math.abs(a[3] - b[3]) < 1.0;
    }
}