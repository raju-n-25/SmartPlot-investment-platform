package com.example.spip_draft.plotintelligence.util;

import com.example.spip_draft.plotintelligence.dto.ContourInputDto;
import com.example.spip_draft.plotintelligence.dto.ContourPoint;
import com.example.spip_draft.plotintelligence.enums.ShapeType;

import java.util.ArrayList;
import java.util.List;

public final class ContourParser {

    private ContourParser() {}

    public static List<double[]> toPointList(ContourInputDto input) {
        if (input == null) {
            throw new IllegalArgumentException("Contour input cannot be null");
        }

        if (input.getPoints() != null && !input.getPoints().isEmpty()) {
            List<double[]> pts = new ArrayList<>();
            for (ContourPoint p : input.getPoints()) {
                if (p != null && p.getX() != null && p.getY() != null) {
                    pts.add(new double[]{p.getX(), p.getY()});
                }
            }
            if (!pts.isEmpty()) {
                return pts;
            }
        }

        if (input.getBboxX() != null && input.getBboxY() != null
                && input.getBboxWidth() != null && input.getBboxHeight() != null) {
            return rectangleFromBbox(
                    input.getBboxX(),
                    input.getBboxY(),
                    input.getBboxWidth(),
                    input.getBboxHeight()
            );
        }

        if (input.getCornerPointsJson() != null && !input.getCornerPointsJson().isBlank()) {
            return parse(input.getCornerPointsJson());
        }

        throw new IllegalArgumentException("Contour must contain points, bbox, or cornerPointsJson");
    }

    public static List<double[]> rectangleFromBbox(Double x, Double y, Double width, Double height) {
        if (x == null || y == null || width == null || height == null) {
            throw new IllegalArgumentException("Cannot build rectangle: bbox values are missing");
        }

        double x2 = x + width;
        double y2 = y + height;

        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{x, y});
        pts.add(new double[]{x2, y});
        pts.add(new double[]{x2, y2});
        pts.add(new double[]{x, y2});
        return pts;
    }

    public static boolean isValid(List<double[]> points) {
        return points != null && points.size() >= 4;
    }

    public static ShapeType classify(List<double[]> points) {
        if (points == null) {
            return ShapeType.POLYGON;
        }
        return points.size() == 4 ? ShapeType.RECTANGLE : ShapeType.POLYGON;
    }

    public static List<double[]> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        String cleaned = json.trim()
                .replaceAll("\\s+", "")
                .replace("[[", "")
                .replace("]]", "");

        if (cleaned.isBlank()) {
            return List.of();
        }

        String[] pairs = cleaned.split("\\],\\[");
        List<double[]> points = new ArrayList<>();

        for (String pair : pairs) {
            String p = pair.replace("[", "").replace("]", "");
            String[] xy = p.split(",");
            if (xy.length != 2) {
                continue;
            }
            try {
                double x = Double.parseDouble(xy[0]);
                double y = Double.parseDouble(xy[1]);
                points.add(new double[]{x, y});
            } catch (NumberFormatException ignored) {
            }
        }

        return points;
    }

    public static String toJson(List<double[]> points) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("[")
              .append(GeometryUtils.round2(points.get(i)[0]))
              .append(",")
              .append(GeometryUtils.round2(points.get(i)[1]))
              .append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<ContourPoint> toDtoPoints(List<double[]> raw) {
        List<ContourPoint> out = new ArrayList<>();
        for (double[] p : raw) {
            out.add(ContourPoint.builder()
                    .x(p[0])
                    .y(p[1])
                    .build());
        }
        return out;
    }
}