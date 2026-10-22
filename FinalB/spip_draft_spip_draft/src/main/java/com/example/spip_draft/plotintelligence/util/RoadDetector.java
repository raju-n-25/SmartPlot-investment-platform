package com.example.spip_draft.plotintelligence.util;

import com.example.spip_draft.plotintelligence.enums.FacingDirection;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Encapsulates all road-detection approaches defined in Module 7.2.
 */
@Slf4j
public final class RoadDetector {

    private RoadDetector() {}

    public static FacingDirection detect(double[] plotBbox,
                                         List<double[]> allPlotBboxes,
                                         double imgWidth,
                                         double imgHeight,
                                         List<double[]> roadBboxes,
                                         String userInputDir,
                                         double boundaryTol,
                                         double roadIntersectTol,
                                         double edgeSharingTol) {

        FacingDirection result = DirectionUtils.detectCombined(
                plotBbox, allPlotBboxes, roadBboxes,
                imgWidth, imgHeight,
                boundaryTol, roadIntersectTol, edgeSharingTol);

        if (result != FacingDirection.UNKNOWN) {
            log.debug("[RoadDetector] Combined detection → {}", result);
            return result;
        }

        if (userInputDir != null && !userInputDir.isBlank()) {
            result = DirectionUtils.fromUserInput(userInputDir);
            log.debug("[RoadDetector] Approach 3 (user input '{}') → {}", userInputDir, result);
            return result;
        }

        log.debug("[RoadDetector] All approaches exhausted → UNKNOWN");
        return FacingDirection.UNKNOWN;
    }

    /**
     * Legacy overload for backward compatibility.
     */
    public static FacingDirection detect(double[] plotBbox,
                                         double imgWidth,
                                         double imgHeight,
                                         List<double[]> roadBboxes,
                                         String userInputDir,
                                         double boundaryTol,
                                         double roadIntersectTol) {

        FacingDirection result = DirectionUtils.detectFromBoundary(
                plotBbox, imgWidth, imgHeight, boundaryTol);

        if (result != FacingDirection.UNKNOWN) {
            log.debug("[RoadDetector] Approach 1 (boundary) → {}", result);
            return result;
        }

        if (roadBboxes != null && !roadBboxes.isEmpty()) {
            result = DirectionUtils.detectFromRoadContours(plotBbox, roadBboxes, roadIntersectTol);
            if (result != FacingDirection.UNKNOWN) {
                log.debug("[RoadDetector] Approach 2 (road contours) → {}", result);
                return result;
            }
        }

        if (userInputDir != null && !userInputDir.isBlank()) {
            result = DirectionUtils.fromUserInput(userInputDir);
            log.debug("[RoadDetector] Approach 3 (user input '{}') → {}", userInputDir, result);
            return result;
        }

        log.debug("[RoadDetector] All approaches exhausted → UNKNOWN");
        return FacingDirection.UNKNOWN;
    }

    public static FacingDirection detectByBoundary(double[] plotBbox,
                                                   double imgWidth,
                                                   double imgHeight) {
        return DirectionUtils.detectFromBoundary(
                plotBbox, imgWidth, imgHeight, DirectionUtils.BOUNDARY_TOLERANCE_PX);
    }

    public static FacingDirection detectByOpenSpace(double[] plotBbox,
                                                    List<double[]> allPlotBboxes,
                                                    double imgWidth,
                                                    double imgHeight) {
        return DirectionUtils.detectFromOpenSpace(
                plotBbox, allPlotBboxes, imgWidth, imgHeight,
                DirectionUtils.SHARED_EDGE_TOLERANCE_PX);
    }

    public static FacingDirection detectByRoadContours(double[] plotBbox,
                                                       List<double[]> roadBboxes) {
        return DirectionUtils.detectFromRoadContours(
                plotBbox, roadBboxes, DirectionUtils.BOUNDARY_TOLERANCE_PX);
    }

    public static FacingDirection detectByUserInput(String userInput) {
        return DirectionUtils.fromUserInput(userInput);
    }
}