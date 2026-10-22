package com.example.spip_draft.plotintelligence.util;

import java.util.ArrayList;
import java.util.List;

public final class GeometryUtils {

    private GeometryUtils() {}

    public static double polygonArea(List<double[]> points) {
        if (points == null || points.size() < 3) return 0.0;

        double sum = 0.0;
        for (int i = 0; i < points.size(); i++) {
            double[] p1 = points.get(i);
            double[] p2 = points.get((i + 1) % points.size());
            sum += (p1[0] * p2[1]) - (p2[0] * p1[1]);
        }
        return Math.abs(sum) / 2.0;
    }

    public static double shoelaceArea(List<double[]> points) {
        return polygonArea(points);
    }

    public static double rectangleArea(double width, double height) {
        if (width <= 0 || height <= 0) return 0.0;
        return width * height;
    }

    public static double toSqft(double pixelArea, double scaleFactorPxPerSqft) {
        if (scaleFactorPxPerSqft <= 0) return 0.0;
        return round2(pixelArea / scaleFactorPxPerSqft);
    }

    public static double sqftToSqm(double sqft) {
        return round2(sqft * 0.092903);
    }

    public static boolean looksRectangle(List<double[]> points, Boolean rectangleCandidate) {
        if (Boolean.TRUE.equals(rectangleCandidate)) return true;
        if (points == null || points.size() != 4) return false;

        double[] p0 = points.get(0);
        double[] p1 = points.get(1);
        double[] p2 = points.get(2);
        double[] p3 = points.get(3);

        boolean topBottomHorizontal =
                nearlyEqual(p0[1], p1[1]) && nearlyEqual(p2[1], p3[1]);

        boolean leftRightVertical =
                nearlyEqual(p0[0], p3[0]) && nearlyEqual(p1[0], p2[0]);

        return topBottomHorizontal && leftRightVertical;
    }

    private static boolean nearlyEqual(double a, double b) {
        return Math.abs(a - b) <= 3.0;
    }

    public static double[] boundingBox(List<double[]> points) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (double[] p : points) {
            if (p[0] < minX) minX = p[0];
            if (p[1] < minY) minY = p[1];
            if (p[0] > maxX) maxX = p[0];
            if (p[1] > maxY) maxY = p[1];
        }

        return new double[]{minX, minY, maxX, maxY};
    }

    public static double[] centroid(List<double[]> points) {
        double sumX = 0, sumY = 0;
        for (double[] p : points) {
            sumX += p[0];
            sumY += p[1];
        }
        return new double[]{sumX / points.size(), sumY / points.size()};
    }

    public static List<double[]> normalise(List<double[]> points, double imageWidth, double imageHeight) {
        List<double[]> out = new ArrayList<>();
        for (double[] p : points) {
            out.add(new double[]{
                    imageWidth > 0 ? p[0] / imageWidth : 0.0,
                    imageHeight > 0 ? p[1] / imageHeight : 0.0
            });
        }
        return out;
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}