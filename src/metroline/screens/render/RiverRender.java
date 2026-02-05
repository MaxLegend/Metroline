package metroline.screens.render;

import metroline.objects.gameobjects.PathPoint;
import metroline.objects.gameobjects.River;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders rivers with smooth blue gradient appearance
 */
public class RiverRender {

    private static final Map<String, CachedRiver> riverCache = new ConcurrentHashMap<>();

    private static final Color RIVER_COLOR = new Color(70, 130, 180, 255);

    private static class CachedRiver {
        public final Shape shape;

        public CachedRiver(Shape shape) {
            this.shape = shape;
        }
    }

    /**
     * Draw river with single solid color
     */
    public static void drawRiver(River river, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        List<PathPoint> path = river.getCalculatedPath();
        if (path.size() < 2) return;

        String cacheKey = createCacheKey(river, offsetX, offsetY, zoom);
        CachedRiver cached = riverCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedRiver(river, offsetX, offsetY, zoom);
            riverCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw single color river
        g2d.setColor(RIVER_COLOR);
        g2d.fill(cached.shape);
    }

    private static CachedRiver createCachedRiver(River river, int offsetX, int offsetY, float zoom) {
        // Width multiplied by 5
        float baseWidth = river.getWidth() * 1.1f * zoom;

        List<PathPoint> path = river.getCalculatedPath();

        Shape shape = createStrokedShape(path, offsetX, offsetY, zoom, baseWidth);

        return new CachedRiver(shape);
    }

    private static Shape createStrokedShape(List<PathPoint> path, int offsetX, int offsetY,
            float zoom, float width) {
        GeneralPath pathShape = createPathShape(path, offsetX, offsetY, zoom);
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(pathShape);
    }

    private static GeneralPath createPathShape(List<PathPoint> path, int offsetX, int offsetY, float zoom) {
        GeneralPath pathShape = new GeneralPath();

        if (path.isEmpty()) return pathShape;

        PathPoint first = path.get(0);
        int startX = (int)((first.getX() * 32 + offsetX + 16) * zoom);
        int startY = (int)((first.getY() * 32 + offsetY + 16) * zoom);
        pathShape.moveTo(startX, startY);

        for (int i = 1; i < path.size(); i++) {
            PathPoint current = path.get(i);
            int x = (int)((current.getX() * 32 + offsetX + 16) * zoom);
            int y = (int)((current.getY() * 32 + offsetY + 16) * zoom);
            pathShape.lineTo(x, y);
        }

        return pathShape;
    }

    /**
     * Draw selection highlight for river
     */
    public static void drawRiverSelection(River river, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        List<PathPoint> path = river.getCalculatedPath();
        if (path.size() < 2) return;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Selection glow
        Shape selectionShape = createStrokedShape(path, offsetX, offsetY, zoom,
                (river.getWidth() + 10) * zoom);
        g2d.setColor(new Color(255, 255, 0, 60));
        g2d.fill(selectionShape);

        // Selection border
        GeneralPath pathLine = createPathShape(path, offsetX, offsetY, zoom);
        g2d.setColor(new Color(255, 220, 50));
        g2d.setStroke(new BasicStroke(3 * zoom, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(pathLine);
    }

    private static String createCacheKey(River river, int offsetX, int offsetY, float zoom) {
        StringBuilder key = new StringBuilder();
        key.append(river.getUniqueId())
           .append("_").append(offsetX)
           .append("_").append(offsetY)
           .append("_").append((int)(zoom * 100))
           .append("_").append((int)(river.getWidth() * 10))
           .append("_").append(river.getCalculatedPath().size());

        // Include path hash
        int hash = 0;
        for (PathPoint p : river.getCalculatedPath()) {
            hash = hash * 31 + p.getX() * 1000 + p.getY();
        }
        key.append("_").append(hash);

        return key.toString();
    }

    public static void clearCache() {
        riverCache.clear();
    }

    public static void clearCacheForRiver(River river) {
        String prefix = String.valueOf(river.getUniqueId());
        riverCache.keySet().removeIf(k -> k.startsWith(prefix));
    }
}