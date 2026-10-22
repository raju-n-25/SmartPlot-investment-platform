package com.example.spip_draft.opencv.service;

import com.example.spip_draft.opencv.config.OpenCvProperties;
import com.example.spip_draft.opencv.dto.DetectedPlotDto;
import com.example.spip_draft.opencv.dto.LayoutProcessingResponse;
import com.example.spip_draft.opencv.entity.DetectedPlot;
import com.example.spip_draft.opencv.entity.LayoutFile;
import com.example.spip_draft.opencv.entity.LayoutProcessingResult;
import com.example.spip_draft.opencv.enums.LayoutProcessingStatus;
import com.example.spip_draft.opencv.repository.DetectedPlotRepository;
import com.example.spip_draft.opencv.repository.LayoutFileRepository;
import com.example.spip_draft.opencv.repository.LayoutProcessingResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * v4: A robust plot-cell detector that supports both:
 *
 *   A) Aerial layout renders / screenshots
 *      - dark background, bright grid lines and roads
 *      - uses LOCAL ADAPTIVE thresholding on a blurred grayscale image
 *
 *   B) White blueprint / 2D plan images
 *      - white background, black grid lines
 *      - uses FIXED dark-line thresholding
 *
 * Automatic mode selection:
 *   meanBrightness > 180  → Blueprint mode
 *   otherwise             → Aerial mode
 *
 * Detection strategy:
 *   1. Convert to grayscale
 *   2. Build a "barrier map" (grid lines / roads / dark borders)
 *   3. Flood-fill all non-barrier connected white-ish interior regions
 *   4. Keep components that satisfy size, fill-ratio, and aspect-ratio rules
 *   5. Emit bounding-box-based DetectedPlot records
 *
 * This approach is much more reliable than plain contour edge detection for
 * standard plotted layouts where each plot is a closed cell.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenCvPipelineService {

    private final OpenCvProperties props;
    private final LayoutFileRepository layoutFileRepository;
    private final LayoutProcessingResultRepository resultRepository;
    private final DetectedPlotRepository detectedPlotRepository;
    private final LayoutStorageService storageService;

    public LayoutProcessingResponse process(LayoutFile layoutFile, Double scaleFactorPxPerSqft) {
        long started = System.currentTimeMillis();

        layoutFile.setProcessingStatus(LayoutProcessingStatus.PROCESSING);
        layoutFile.setProcessingError(null);
        layoutFileRepository.save(layoutFile);

        try {
            Path imagePath = Paths.get(
                    layoutFile.getConvertedImagePath() != null
                            ? layoutFile.getConvertedImagePath()
                            : layoutFile.getFilePath()
            ).toAbsolutePath().normalize();

            BufferedImage src = ImageIO.read(imagePath.toFile());
            if (src == null) {
                throw new IllegalStateException("Unable to read image for layout file id=" + layoutFile.getId());
            }

            int width = src.getWidth();
            int height = src.getHeight();
            int totalPixels = width * height;

            log.info("[OpenCV-v4] Processing layoutFile={} image={} ({}x{})",
                    layoutFile.getId(), imagePath.getFileName(), width, height);

            int[][] gray = toGrayscale(src);
            BufferedImage grayImg = toGrayImage(gray);
            Path grayPath = storageService.writeDebugImage(
                    layoutFile.getProject().getId(),
                    "layout-" + layoutFile.getId(),
                    "grayscale",
                    grayImg
            );

            double mean = mean(gray);
            boolean blueprintMode = mean > 180.0;
            log.info("[OpenCV-v4] Auto-mode selected: {} (mean brightness = {})",
                    blueprintMode ? "BLUEPRINT" : "AERIAL", round2(mean));

            boolean[][] barrier = blueprintMode
                    ? buildBlueprintBarrier(gray)
                    : buildAerialBarrier(gray);

            BufferedImage barrierImg = drawBinary(barrier, width, height, true);
            Path edgePath = storageService.writeDebugImage(
                    layoutFile.getProject().getId(),
                    "layout-" + layoutFile.getId(),
                    blueprintMode ? "barrier-blueprint" : "barrier-aerial",
                    barrierImg
            );

            List<Component> components = floodComponents(barrier, width, height);
            log.info("[OpenCV-v4] Connected components found: {}", components.size());

            double minAreaPx = props.getMinPlotAreaPx();
            double maxAreaPx = props.getMaxPlotAreaRatio() * totalPixels;

            List<Component> kept = new ArrayList<>();
            for (Component c : components) {
                double area = c.pixelCount;
                int bw = c.maxX - c.minX + 1;
                int bh = c.maxY - c.minY + 1;
                double rectArea = (double) bw * bh;
                double fillRatio = rectArea <= 0 ? 0 : area / rectArea;
                double aspect = bh == 0 ? 999 : Math.max((double) bw / bh, (double) bh / bw);

                boolean ok =
                        area >= minAreaPx &&
                                area <= maxAreaPx &&
                                fillRatio >= props.getMinRectangularity() &&
                                aspect <= props.getMaxAspectRatio();

                if (ok) kept.add(c);
            }

            kept.sort(Comparator
                    .comparingInt((Component c) -> c.minY)
                    .thenComparingInt(c -> c.minX));

            log.info("[OpenCV-v4] Components kept as plots: {}", kept.size());

            BufferedImage contourDebug = drawBoundingBoxes(src, kept, new Color(0, 255, 255), 2, true);
            Path contourPath = storageService.writeDebugImage(
                    layoutFile.getProject().getId(),
                    "layout-" + layoutFile.getId(),
                    "contours",
                    contourDebug
            );

            BufferedImage finalDebug = drawBoundingBoxes(src, kept, new Color(0, 255, 0), 2, false);
            Path finalPath = storageService.writeDebugImage(
                    layoutFile.getProject().getId(),
                    "layout-" + layoutFile.getId(),
                    "filtered-plots",
                    finalDebug
            );

            LayoutProcessingResult result = resultRepository.findByLayoutFileId(layoutFile.getId())
                    .orElseGet(() -> LayoutProcessingResult.builder().layoutFile(layoutFile).build());

            result.setScaleFactorPxPerSqft(
                    (scaleFactorPxPerSqft != null && scaleFactorPxPerSqft > 0)
                            ? scaleFactorPxPerSqft
                            : props.getDefaultScaleFactorPxPerSqft()
            );
            result.setImageWidth(width);
            result.setImageHeight(height);
            result.setTotalPlotsDetected(kept.size());
            result.setDebugGrayscalePath(storageService.toRelativePath(grayPath));
            result.setDebugEdgePath(storageService.toRelativePath(edgePath));
            result.setDebugContourPath(storageService.toRelativePath(contourPath));
            result.setDebugFilteredPlotsPath(storageService.toRelativePath(finalPath));

            result.getDetectedPlots().clear();

            List<DetectedPlotDto> dtoList = new ArrayList<>();
            int index = 1;
            for (Component c : kept) {
                int bw = c.maxX - c.minX + 1;
                int bh = c.maxY - c.minY + 1;

                double pxArea = c.pixelCount;
                double sqft = pxArea / result.getScaleFactorPxPerSqft();
                double sqm = sqft * 0.092903;

                String cornerJson = String.format(Locale.US,
                        "[[%d,%d],[%d,%d],[%d,%d],[%d,%d]]",
                        c.minX, c.minY,
                        c.maxX, c.minY,
                        c.maxX, c.maxY,
                        c.minX, c.maxY
                );

                DetectedPlot entity = DetectedPlot.builder()
                        .processingResult(result)
                        .plotIndex(index)
                        .bboxX(c.minX)
                        .bboxY(c.minY)
                        .bboxWidth(bw)
                        .bboxHeight(bh)
                        .pixelArea(pxArea)
                        .areaSqft(round2(sqft))
                        .areaSqm(round2(sqm))
                        .isRectangle(true)
                        .cornerPointsJson(cornerJson)
                        .build();

                result.getDetectedPlots().add(entity);

                dtoList.add(DetectedPlotDto.builder()
                        .plotIndex(index)
                        .bboxX(c.minX)
                        .bboxY(c.minY)
                        .bboxWidth(bw)
                        .bboxHeight(bh)
                        .pixelArea(round2(pxArea))
                        .areaSqft(round2(sqft))
                        .areaSqm(round2(sqm))
                        .isRectangle(true)
                        .cornerPointsJson(cornerJson)
                        .build());

                index++;
            }

            result = resultRepository.save(result);

            List<DetectedPlot> savedPlots = detectedPlotRepository.findByProcessingResultIdOrderByPlotIndexAsc(result.getId());
            for (int i = 0; i < Math.min(savedPlots.size(), dtoList.size()); i++) {
                dtoList.get(i).setId(savedPlots.get(i).getId());
            }

            layoutFile.setProcessingStatus(LayoutProcessingStatus.COMPLETED);
            layoutFile.setProcessingError(null);
            layoutFileRepository.save(layoutFile);

            log.info("[OpenCV-v4] Completed layoutFile={} in {} ms, plots={}",
                    layoutFile.getId(), (System.currentTimeMillis() - started), dtoList.size());

            return LayoutProcessingResponse.builder()
                    .layoutFileId(layoutFile.getId())
                    .processingResultId(result.getId())
                    .projectId(layoutFile.getProject().getId())
                    .processingStatus(layoutFile.getProcessingStatus())
                    .processingError(null)
                    .totalPlotsDetected(dtoList.size())
                    .scaleFactorPxPerSqft(result.getScaleFactorPxPerSqft())
                    .debugGrayscalePath(result.getDebugGrayscalePath())
                    .debugEdgePath(result.getDebugEdgePath())
                    .debugContourPath(result.getDebugContourPath())
                    .debugFilteredPlotsPath(result.getDebugFilteredPlotsPath())
                    .imageWidth(result.getImageWidth())
                    .imageHeight(result.getImageHeight())
                    .detectedPlots(dtoList)
                    .build();

        } catch (Exception ex) {
            layoutFile.setProcessingStatus(LayoutProcessingStatus.FAILED);
            layoutFile.setProcessingError(ex.getMessage());
            layoutFileRepository.save(layoutFile);

            log.error("[OpenCV-v4] Processing failed for layoutFile={}: {}", layoutFile.getId(), ex.getMessage(), ex);

            return LayoutProcessingResponse.builder()
                    .layoutFileId(layoutFile.getId())
                    .projectId(layoutFile.getProject().getId())
                    .processingStatus(LayoutProcessingStatus.FAILED)
                    .processingError(ex.getMessage())
                    .detectedPlots(Collections.emptyList())
                    .build();
        }
    }

    // ========= MODE A: AERIAL =========

    private boolean[][] buildAerialBarrier(int[][] gray) {
        int h = gray.length;
        int w = gray[0].length;

        int r = Math.max(3, props.getAdaptiveRadius());
        double sigma = Math.max(1.0, r / 4.0);

        int[][] blurred = gaussianBlur(gray, sigma, r);
        double[][] localMean = boxMean(blurred, r);

        boolean[][] barrier = new boolean[h][w];
        double offset = props.getCannyThreshold1();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                barrier[y][x] = (gray[y][x] - localMean[y][x]) > offset;
            }
        }
        return barrier;
    }

    // ========= MODE B: BLUEPRINT =========

    private boolean[][] buildBlueprintBarrier(int[][] gray) {
        int h = gray.length;
        int w = gray[0].length;

        boolean[][] barrier = new boolean[h][w];
        int darkThresh = props.getBlueprintDarkThresh();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                barrier[y][x] = gray[y][x] < darkThresh;
            }
        }
        return barrier;
    }

    // ========= CONNECTED COMPONENTS =========

    private List<Component> floodComponents(boolean[][] barrier, int width, int height) {
        boolean[][] visited = new boolean[height][width];
        List<Component> components = new ArrayList<>();

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (barrier[y][x] || visited[y][x]) continue;

                Component c = new Component();
                ArrayDeque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                visited[y][x] = true;

                while (!q.isEmpty()) {
                    int[] p = q.removeFirst();
                    int px = p[0], py = p[1];

                    c.pixelCount++;
                    if (px < c.minX) c.minX = px;
                    if (py < c.minY) c.minY = py;
                    if (px > c.maxX) c.maxX = px;
                    if (py > c.maxY) c.maxY = py;

                    for (int k = 0; k < 4; k++) {
                        int nx = px + dx[k];
                        int ny = py + dy[k];
                        if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
                        if (visited[ny][nx] || barrier[ny][nx]) continue;

                        visited[ny][nx] = true;
                        q.addLast(new int[]{nx, ny});
                    }
                }

                components.add(c);
            }
        }
        return components;
    }

    // ========= IMAGE HELPERS =========

    private int[][] toGrayscale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[][] gray = new int[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[y][x] = (r * 299 + g * 587 + b * 114) / 1000;
            }
        }
        return gray;
    }

    private BufferedImage toGrayImage(int[][] gray) {
        int h = gray.length;
        int w = gray[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = clamp(gray[y][x], 0, 255);
                int rgb = (v << 16) | (v << 8) | v;
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    private BufferedImage drawBinary(boolean[][] binary, int width, int height, boolean barrierWhite) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int on = barrierWhite ? 0xFFFFFF : 0x000000;
        int off = barrierWhite ? 0x000000 : 0xFFFFFF;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, binary[y][x] ? on : off);
            }
        }
        return img;
    }

    private BufferedImage drawBoundingBoxes(BufferedImage src, List<Component> boxes, Color color, int stroke, boolean withIndex) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(src, 0, 0, null);
            g.setColor(color);
            g.setStroke(new BasicStroke(stroke));
            g.setFont(new Font("Arial", Font.BOLD, 18));

            int idx = 1;
            for (Component c : boxes) {
                int x = c.minX;
                int y = c.minY;
                int w = c.maxX - c.minX + 1;
                int h = c.maxY - c.minY + 1;
                g.drawRect(x, y, w, h);
                if (withIndex) {
                    g.drawString(String.valueOf(idx++), x + 4, Math.max(18, y + 18));
                }
            }
        } finally {
            g.dispose();
        }
        return out;
    }

    private double mean(int[][] gray) {
        long sum = 0;
        int h = gray.length;
        int w = gray[0].length;
        for (int[] row : gray) for (int v : row) sum += v;
        return (double) sum / (w * h);
    }

    // ========= BLUR / LOCAL MEAN =========

    private int[][] gaussianBlur(int[][] src, double sigma, int radius) {
        double[] kernel = gaussianKernel1D(radius, sigma);
        int h = src.length;
        int w = src[0].length;

        double[][] temp = new double[h][w];
        int[][] out = new int[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double sum = 0;
                for (int k = -radius; k <= radius; k++) {
                    int xx = clamp(x + k, 0, w - 1);
                    sum += src[y][xx] * kernel[k + radius];
                }
                temp[y][x] = sum;
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double sum = 0;
                for (int k = -radius; k <= radius; k++) {
                    int yy = clamp(y + k, 0, h - 1);
                    sum += temp[yy][x] * kernel[k + radius];
                }
                out[y][x] = clamp((int) Math.round(sum), 0, 255);
            }
        }
        return out;
    }

    private double[] gaussianKernel1D(int radius, double sigma) {
        double[] k = new double[2 * radius + 1];
        double sum = 0;
        for (int i = -radius; i <= radius; i++) {
            double v = Math.exp(-(i * i) / (2 * sigma * sigma));
            k[i + radius] = v;
            sum += v;
        }
        for (int i = 0; i < k.length; i++) k[i] /= sum;
        return k;
    }

    private double[][] boxMean(int[][] src, int radius) {
        int h = src.length;
        int w = src[0].length;

        long[][] integral = new long[h + 1][w + 1];
        for (int y = 1; y <= h; y++) {
            long rowSum = 0;
            for (int x = 1; x <= w; x++) {
                rowSum += src[y - 1][x - 1];
                integral[y][x] = integral[y - 1][x] + rowSum;
            }
        }

        double[][] out = new double[h][w];
        for (int y = 0; y < h; y++) {
            int y1 = Math.max(0, y - radius);
            int y2 = Math.min(h - 1, y + radius);
            for (int x = 0; x < w; x++) {
                int x1 = Math.max(0, x - radius);
                int x2 = Math.min(w - 1, x + radius);

                long sum = integral[y2 + 1][x2 + 1]
                        - integral[y1][x2 + 1]
                        - integral[y2 + 1][x1]
                        + integral[y1][x1];

                int area = (y2 - y1 + 1) * (x2 - x1 + 1);
                out[y][x] = (double) sum / area;
            }
        }
        return out;
    }

    // ========= SMALL HELPERS =========

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class Component {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int pixelCount = 0;
    }
}