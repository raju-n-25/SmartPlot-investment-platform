package com.example.spip_draft.plotintelligence.util;

import com.example.spip_draft.plotintelligence.enums.FacingDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for mapping image-space edges to domain-facing directions.
 *
 * Assumptions:
 *   - by default image top = NORTH, right = EAST, bottom = SOUTH, left = WEST
 *   - orientation remapping can be applied before using these helpers if needed
 *
 * Boundary-based direction logic:
 *   If a plot touches the outer image boundary, that touching side is assumed
 *   road/open-facing unless overridden by more specific road contour logic.
 */
public final class DirectionUtils {

    private DirectionUtils() {}

    public static final double BOUNDARY_TOLERANCE_PX = 10.0;
    public static final double SHARED_EDGE_TOLERANCE_PX = 10.0;

    public static FacingDirection fromUserInput(String value) {
        if (value == null || value.isBlank()) return FacingDirection.UNKNOWN;

        String v = value.trim().toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        return switch (v) {
            case "N", "NORTH" -> FacingDirection.NORTH;
            case "S", "SOUTH" -> FacingDirection.SOUTH;
            case "E", "EAST" -> FacingDirection.EAST;
            case "W", "WEST" -> FacingDirection.WEST;
            case "NE", "NORTH_EAST", "NORTHEAST" -> FacingDirection.NORTH_EAST;
            case "NW", "NORTH_WEST", "NORTHWEST" -> FacingDirection.NORTH_WEST;
            case "SE", "SOUTH_EAST", "SOUTHEAST" -> FacingDirection.SOUTH_EAST;
            case "SW", "SOUTH_WEST", "SOUTHWEST" -> FacingDirection.SOUTH_WEST;
            default -> FacingDirection.UNKNOWN;
        };
    }

    public static FacingDirection detectFromBoundary(double[] bbox,
                                                     double imageWidth,
                                                     double imageHeight,
                                                     double tolerancePx) {
        boolean top    = bbox[1] <= tolerancePx;
        boolean left   = bbox[0] <= tolerancePx;
        boolean right  = (imageWidth - bbox[2]) <= tolerancePx;
        boolean bottom = (imageHeight - bbox[3]) <= tolerancePx;

        return combine(top, right, bottom, left);
    }

    public static FacingDirection detectFromRoadContours(double[] plotBbox,
                                                         List<double[]> roadBboxes,
                                                         double tolerancePx) {
        if (roadBboxes == null || roadBboxes.isEmpty()) {
            return FacingDirection.UNKNOWN;
        }

        boolean top = false, right = false, bottom = false, left = false;

        for (double[] road : roadBboxes) {
            if (intersectsTop(plotBbox, road, tolerancePx)) top = true;
            if (intersectsRight(plotBbox, road, tolerancePx)) right = true;
            if (intersectsBottom(plotBbox, road, tolerancePx)) bottom = true;
            if (intersectsLeft(plotBbox, road, tolerancePx)) left = true;
        }

        return combine(top, right, bottom, left);
    }

    /**
     * Detect open-space facing by checking which edges of the plot do NOT have
     * an overlapping neighboring plot on that side.
     */
    public static FacingDirection detectFromOpenSpace(double[] plotBbox,
                                                      List<double[]> allPlotBboxes,
                                                      double imageWidth,
                                                      double imageHeight,
                                                      double sharedEdgeTolerancePx) {

        boolean topOpen = true;
        boolean rightOpen = true;
        boolean bottomOpen = true;
        boolean leftOpen = true;

        for (double[] other : allPlotBboxes) {
            if (sameBox(plotBbox, other)) continue;

            if (sharesTop(plotBbox, other, sharedEdgeTolerancePx)) {
                topOpen = false;
            }
            if (sharesRight(plotBbox, other, sharedEdgeTolerancePx)) {
                rightOpen = false;
            }
            if (sharesBottom(plotBbox, other, sharedEdgeTolerancePx)) {
                bottomOpen = false;
            }
            if (sharesLeft(plotBbox, other, sharedEdgeTolerancePx)) {
                leftOpen = false;
            }
        }

        if (plotBbox[1] <= sharedEdgeTolerancePx) topOpen = true;
        if (plotBbox[0] <= sharedEdgeTolerancePx) leftOpen = true;
        if ((imageWidth - plotBbox[2]) <= sharedEdgeTolerancePx) rightOpen = true;
        if ((imageHeight - plotBbox[3]) <= sharedEdgeTolerancePx) bottomOpen = true;

        return combine(topOpen, rightOpen, bottomOpen, leftOpen);
    }

    public static FacingDirection detectCombined(double[] plotBbox,
                                                 List<double[]> allPlotBboxes,
                                                 List<double[]> roadBboxes,
                                                 double imageWidth,
                                                 double imageHeight,
                                                 double boundaryTol,
                                                 double roadIntersectTol,
                                                 double edgeSharingTol) {

        boolean top = false, right = false, bottom = false, left = false;

        if (plotBbox[1] <= boundaryTol) top = true;
        if (plotBbox[0] <= boundaryTol) left = true;
        if ((imageWidth - plotBbox[2]) <= boundaryTol) right = true;
        if ((imageHeight - plotBbox[3]) <= boundaryTol) bottom = true;

        if (allPlotBboxes != null && !allPlotBboxes.isEmpty()) {
            boolean topHasNeighbour = false;
            boolean rightHasNeighbour = false;
            boolean bottomHasNeighbour = false;
            boolean leftHasNeighbour = false;

            for (double[] other : allPlotBboxes) {
                if (sameBox(plotBbox, other)) continue;

                if (sharesTop(plotBbox, other, edgeSharingTol)) topHasNeighbour = true;
                if (sharesRight(plotBbox, other, edgeSharingTol)) rightHasNeighbour = true;
                if (sharesBottom(plotBbox, other, edgeSharingTol)) bottomHasNeighbour = true;
                if (sharesLeft(plotBbox, other, edgeSharingTol)) leftHasNeighbour = true;
            }

            if (!topHasNeighbour) top = true;
            if (!rightHasNeighbour) right = true;
            if (!bottomHasNeighbour) bottom = true;
            if (!leftHasNeighbour) left = true;
        }

        if (roadBboxes != null && !roadBboxes.isEmpty()) {
            for (double[] road : roadBboxes) {
                if (intersectsTop(plotBbox, road, roadIntersectTol)) top = true;
                if (intersectsRight(plotBbox, road, roadIntersectTol)) right = true;
                if (intersectsBottom(plotBbox, road, roadIntersectTol)) bottom = true;
                if (intersectsLeft(plotBbox, road, roadIntersectTol)) left = true;
            }
        }

        return combine(top, right, bottom, left);
    }

    public static FacingDirection remapForOrientation(FacingDirection rawDirection, String orientation) {
        if (rawDirection == null || rawDirection == FacingDirection.UNKNOWN) {
            return FacingDirection.UNKNOWN;
        }

        String o = (orientation == null ? "NORTH_UP" : orientation.trim().toUpperCase(Locale.ROOT));

        if ("NORTH_UP".equals(o)) {
            return rawDirection;
        }

        return switch (o) {
            case "EAST_UP" -> rotate90CounterClockwise(rawDirection);
            case "SOUTH_UP" -> rotate180(rawDirection);
            case "WEST_UP" -> rotate90Clockwise(rawDirection);
            default -> rawDirection;
        };
    }

    private static FacingDirection rotate90Clockwise(FacingDirection d) {
        return switch (d) {
            case NORTH -> FacingDirection.EAST;
            case EAST -> FacingDirection.SOUTH;
            case SOUTH -> FacingDirection.WEST;
            case WEST -> FacingDirection.NORTH;
            case NORTH_EAST -> FacingDirection.SOUTH_EAST;
            case SOUTH_EAST -> FacingDirection.SOUTH_WEST;
            case SOUTH_WEST -> FacingDirection.NORTH_WEST;
            case NORTH_WEST -> FacingDirection.NORTH_EAST;
            default -> FacingDirection.UNKNOWN;
        };
    }

    private static FacingDirection rotate90CounterClockwise(FacingDirection d) {
        return switch (d) {
            case NORTH -> FacingDirection.WEST;
            case WEST -> FacingDirection.SOUTH;
            case SOUTH -> FacingDirection.EAST;
            case EAST -> FacingDirection.NORTH;
            case NORTH_EAST -> FacingDirection.NORTH_WEST;
            case NORTH_WEST -> FacingDirection.SOUTH_WEST;
            case SOUTH_WEST -> FacingDirection.SOUTH_EAST;
            case SOUTH_EAST -> FacingDirection.NORTH_EAST;
            default -> FacingDirection.UNKNOWN;
        };
    }

    private static FacingDirection rotate180(FacingDirection d) {
        return switch (d) {
            case NORTH -> FacingDirection.SOUTH;
            case SOUTH -> FacingDirection.NORTH;
            case EAST -> FacingDirection.WEST;
            case WEST -> FacingDirection.EAST;
            case NORTH_EAST -> FacingDirection.SOUTH_WEST;
            case NORTH_WEST -> FacingDirection.SOUTH_EAST;
            case SOUTH_EAST -> FacingDirection.NORTH_WEST;
            case SOUTH_WEST -> FacingDirection.NORTH_EAST;
            default -> FacingDirection.UNKNOWN;
        };
    }

    public static boolean isRoadFacing(FacingDirection direction) {
        return direction != null && direction != FacingDirection.UNKNOWN;
    }

    public static boolean isCornerPlot(FacingDirection direction) {
        return direction != null && direction.isCornerDirection();
    }

    private static FacingDirection combine(boolean top, boolean right, boolean bottom, boolean left) {
        int count = (top ? 1 : 0) + (right ? 1 : 0) + (bottom ? 1 : 0) + (left ? 1 : 0);

        if (count == 0) return FacingDirection.UNKNOWN;
        if (count == 1) {
            if (top) return FacingDirection.NORTH;
            if (right) return FacingDirection.EAST;
            if (bottom) return FacingDirection.SOUTH;
            return FacingDirection.WEST;
        }

        if (top && right && !bottom && !left) return FacingDirection.NORTH_EAST;
        if (top && left && !bottom && !right) return FacingDirection.NORTH_WEST;
        if (bottom && right && !top && !left) return FacingDirection.SOUTH_EAST;
        if (bottom && left && !top && !right) return FacingDirection.SOUTH_WEST;

        if (top) return FacingDirection.NORTH;
        if (right) return FacingDirection.EAST;
        if (bottom) return FacingDirection.SOUTH;
        if (left) return FacingDirection.WEST;

        return FacingDirection.UNKNOWN;
    }

    private static boolean intersectsTop(double[] plot, double[] road, double tol) {
        double plotTop = plot[1];
        double roadBottom = road[3];
        return Math.abs(plotTop - roadBottom) <= tol && overlap(plot[0], plot[2], road[0], road[2]) > 0;
    }

    private static boolean intersectsRight(double[] plot, double[] road, double tol) {
        double plotRight = plot[2];
        double roadLeft = road[0];
        return Math.abs(plotRight - roadLeft) <= tol && overlap(plot[1], plot[3], road[1], road[3]) > 0;
    }

    private static boolean intersectsBottom(double[] plot, double[] road, double tol) {
        double plotBottom = plot[3];
        double roadTop = road[1];
        return Math.abs(plotBottom - roadTop) <= tol && overlap(plot[0], plot[2], road[0], road[2]) > 0;
    }

    private static boolean intersectsLeft(double[] plot, double[] road, double tol) {
        double plotLeft = plot[0];
        double roadRight = road[2];
        return Math.abs(plotLeft - roadRight) <= tol && overlap(plot[1], plot[3], road[1], road[3]) > 0;
    }

    private static boolean sharesTop(double[] box, double[] other, double tol) {
        return Math.abs(box[1] - other[3]) <= tol && overlap(box[0], box[2], other[0], other[2]) > 0;
    }

    private static boolean sharesRight(double[] box, double[] other, double tol) {
        return Math.abs(box[2] - other[0]) <= tol && overlap(box[1], box[3], other[1], other[3]) > 0;
    }

    private static boolean sharesBottom(double[] box, double[] other, double tol) {
        return Math.abs(box[3] - other[1]) <= tol && overlap(box[0], box[2], other[0], other[2]) > 0;
    }

    private static boolean sharesLeft(double[] box, double[] other, double tol) {
        return Math.abs(box[0] - other[2]) <= tol && overlap(box[1], box[3], other[1], other[3]) > 0;
    }

    private static double overlap(double a1, double a2, double b1, double b2) {
        return Math.max(0, Math.min(a2, b2) - Math.max(a1, b1));
    }

    private static boolean sameBox(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < 0.0001
                && Math.abs(a[1] - b[1]) < 0.0001
                && Math.abs(a[2] - b[2]) < 0.0001
                && Math.abs(a[3] - b[3]) < 0.0001;
    }

    public static List<double[]> convertRoadBoundingBoxes(List<List<Double>> rawRoadBoxes) {
        List<double[]> out = new ArrayList<>();
        if (rawRoadBoxes == null) return out;

        for (List<Double> box : rawRoadBoxes) {
            if (box == null || box.size() != 4) continue;
            if (box.get(0) == null || box.get(1) == null || box.get(2) == null || box.get(3) == null) continue;
            out.add(new double[]{
                    box.get(0), box.get(1), box.get(2), box.get(3)
            });
        }
        return out;
    }
    
    public static List<String> roadFacingEdges(FacingDirection direction) {
        List<String> edges = new ArrayList<>();
        if (direction == null || direction == FacingDirection.UNKNOWN) {
            return edges;
        }

        switch (direction) {
            case NORTH -> edges.add("NORTH");
            case SOUTH -> edges.add("SOUTH");
            case EAST -> edges.add("EAST");
            case WEST -> edges.add("WEST");
            case NORTH_EAST -> {
                edges.add("NORTH");
                edges.add("EAST");
            }
            case NORTH_WEST -> {
                edges.add("NORTH");
                edges.add("WEST");
            }
            case SOUTH_EAST -> {
                edges.add("SOUTH");
                edges.add("EAST");
            }
            case SOUTH_WEST -> {
                edges.add("SOUTH");
                edges.add("WEST");
            }
            default -> {
            }
        }

        return edges;
    }

    public static String facingLabel(FacingDirection direction) {
        if (direction == null || direction == FacingDirection.UNKNOWN) {
            return "UNKNOWN";
        }

        return switch (direction) {
            case NORTH -> "North Facing";
            case SOUTH -> "South Facing";
            case EAST -> "East Facing";
            case WEST -> "West Facing";
            case NORTH_EAST -> "North-East Facing";
            case NORTH_WEST -> "North-West Facing";
            case SOUTH_EAST -> "South-East Facing";
            case SOUTH_WEST -> "South-West Facing";
            default -> "UNKNOWN";
        };
    }
}