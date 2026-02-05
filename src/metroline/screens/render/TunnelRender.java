package metroline.screens.render;

import metroline.objects.gameobjects.PathPoint;
import metroline.objects.gameobjects.Tunnel;
import metroline.objects.enums.TunnelType;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс для отрисовки туннелей с кэшированием геометрии
 */
public class TunnelRender {

    private static final Map<String, CachedTunnel> tunnelCache = new ConcurrentHashMap<>();
    private static final Map<String, Shape> selectionCache = new ConcurrentHashMap<>();
    private static final Map<String, Shape> controlPointCache = new ConcurrentHashMap<>();

    private static class CachedTunnel {
        public final Shape activeShape;
        public final Shape plannedShape;
        public final Shape buildingShape;
        public final Shape destroyedOuterShape;
        public final Shape destroyedInnerShape;

        public CachedTunnel(Shape activeShape, Shape plannedShape, Shape buildingShape,
                Shape destroyedOuterShape, Shape destroyedInnerShape) {
            this.activeShape = activeShape;
            this.plannedShape = plannedShape;
            this.buildingShape = buildingShape;
            this.destroyedOuterShape = destroyedOuterShape;
            this.destroyedInnerShape = destroyedInnerShape;
        }
    }

    /**
     * Отрисовка туннеля с кэшированием
     */
    public static void drawTunnel(Tunnel tunnel, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        if (tunnel.getPath().size() < 2) return;

        String cacheKey = createCacheKey(tunnel, offsetX, offsetY, zoom);
        CachedTunnel cached = tunnelCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedTunnel(tunnel, offsetX, offsetY, zoom);
            tunnelCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (tunnel.getType()) {
            case PLANNED:
                g2d.setColor(tunnel.getStart().getColor());
                g2d.fill(cached.plannedShape);
                break;
            case BUILDING:
                g2d.setColor(tunnel.getStart().getColor());
                g2d.fill(cached.buildingShape);
                break;
            case DESTROYED:
                drawCachedDestroyedTunnel(tunnel, g2d, cached, offsetX, offsetY, zoom);
                break;
            default: // ACTIVE
                g2d.setColor(tunnel.getStart().getColor());
                g2d.fill(cached.activeShape);
        }
    }

    /**
     * Создание кэшированного туннеля
     */
    private static CachedTunnel createCachedTunnel(Tunnel tunnel, int offsetX, int offsetY, float zoom) {
        float baseWidth = 12 * zoom;
        float innerWidth = baseWidth - 4 * zoom;

        Shape activeShape = createStrokedShape(tunnel.getPath(), offsetX, offsetY, zoom, baseWidth);
        Shape plannedShape = createTunnelArea(tunnel, offsetX, offsetY, zoom, baseWidth, innerWidth);
        Shape buildingShape = createBuildingTunnelShape(tunnel, offsetX, offsetY, zoom, baseWidth, innerWidth);

        Shape destroyedOuterShape = createStrokedShape(tunnel.getPath(), offsetX, offsetY, zoom, baseWidth);
        Shape destroyedInnerShape = createStrokedShape(tunnel.getPath(), offsetX, offsetY, zoom, innerWidth);

        return new CachedTunnel(activeShape, plannedShape, buildingShape,
                destroyedOuterShape, destroyedInnerShape);
    }

    /**
     * Отрисовка разрушенного туннеля из кэша
     */
    private static void drawCachedDestroyedTunnel(Tunnel tunnel, Graphics2D g2d, CachedTunnel cached,
            int offsetX, int offsetY, float zoom) {
        // Outer contour
        g2d.setColor(tunnel.getStart().getColor());
        g2d.fill(cached.destroyedOuterShape);

        // Transparent cutout
        Area destroyedArea = new Area(cached.destroyedOuterShape);
        destroyedArea.subtract(new Area(cached.destroyedInnerShape));

        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fill(destroyedArea);
        g2d.setComposite(originalComposite);

        // Inner black stripe with CORRECT zoom
        float coreWidth = (12 * zoom) - (6 * zoom); // Correctly scaled width
        Shape coreShape = createStrokedShape(tunnel.getPath(), offsetX, offsetY, zoom, coreWidth);
        g2d.setColor(Color.BLACK);
        g2d.fill(coreShape);
    }

    /**
     * Создание формы строящегося туннеля
     */
    private static Shape createBuildingTunnelShape(Tunnel tunnel, int offsetX, int offsetY,
            float zoom, float baseWidth, float innerWidth) {
        float dashLength = 4.0f * zoom;
        float gapLength = 4.0f * zoom;

        // Пунктирный внешний контур
        BasicStroke dashedStroke = new BasicStroke(
                baseWidth,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND,
                10f,
                new float[]{dashLength, gapLength},
                0f
        );

        Shape outerDashedShape = dashedStroke.createStrokedShape(
                createPathShape(tunnel.getPath(), offsetX, offsetY, zoom)
        );

        // Сплошной внутренний контур для вырезания
        Shape innerSolidShape = new BasicStroke(innerWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(createPathShape(tunnel.getPath(), offsetX, offsetY, zoom));

        // Вычитаем внутреннюю часть
        Area tunnelArea = new Area(outerDashedShape);
        tunnelArea.subtract(new Area(innerSolidShape));

        return tunnelArea;
    }

    /**
     * Создание области туннеля
     */
    private static Area createTunnelArea(Tunnel tunnel, int offsetX, int offsetY,
            float zoom, float outerWidth, float innerWidth) {
        Area outerArea = new Area(createStrokedShape(tunnel.getPath(), offsetX, offsetY, zoom, outerWidth));
        Area innerArea = new Area(createStrokedShape(tunnel.getPath(), offsetX, offsetY, zoom, innerWidth));
        outerArea.subtract(innerArea);
        return outerArea;
    }

    /**
     * Создание контура с заданной толщиной
     */
    private static Shape createStrokedShape(List<PathPoint> path, int offsetX, int offsetY,
            float zoom, float width) {
        GeneralPath pathShape = createPathShape(path, offsetX, offsetY, zoom);
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f)
                .createStrokedShape(pathShape);
    }

    /**
     * Создание формы пути
     */
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
     * Отрисовка выделения туннеля с кэшированием
     */
    public static void drawTunnelSelection(Tunnel tunnel, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        if (tunnel.getPath().size() < 2) return;

        String cacheKey = "selection_" + createBaseCacheKey(tunnel, offsetX, offsetY, zoom);
        Shape selectionShape = selectionCache.get(cacheKey);

        if (selectionShape == null) {
            selectionShape = createStrokedShape(tunnel.getPath(), offsetX, offsetY, zoom, 15 * zoom);
            selectionCache.put(cacheKey, selectionShape);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 255, 0, 100)); // Полупрозрачный желтый
        g2d.fill(selectionShape);

        // Контур выделения
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        GeneralPath path = createPathShape(tunnel.getPath(), offsetX, offsetY, zoom);
        g2d.draw(path);
    }

    /**
     * Отрисовка точки управления туннелем с кэшированием
     */
    public static void drawControlPoint(Tunnel tunnel, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        if (tunnel.getPathPoint() == null) return;

        String cacheKey = "control_" + tunnel.getPathPoint().getX() + "_" + tunnel.getPathPoint().getY() + "_" + zoom;
        Shape controlShape = controlPointCache.get(cacheKey);

        if (controlShape == null) {
            int x = (int)((tunnel.getPathPoint().getX() * 32 + offsetX + 16) * zoom);
            int y = (int)((tunnel.getPathPoint().getY() * 32 + offsetY + 16) * zoom);
            int size = (int)(8 * zoom);
            controlShape = new java.awt.geom.Ellipse2D.Float(x - size/2, y - size/2, size, size);
            controlPointCache.put(cacheKey, controlShape);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Заливка точки
        g2d.setColor(new Color(255, 255, 0, 180));
        g2d.fill(controlShape);

        // Контур точки
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(1.5f * zoom));
        g2d.draw(controlShape);
    }

    /**
     * Создание ключа для кэша
     */
    private static String createCacheKey(Tunnel tunnel, int offsetX, int offsetY, float zoom) {
        return createBaseCacheKey(tunnel, offsetX, offsetY, zoom) + "_" + tunnel.getType();
    }

    private static String createBaseCacheKey(Tunnel tunnel, int offsetX, int offsetY, float zoom) {
        StringBuilder key = new StringBuilder();
        key.append("tunnel_").append(tunnel.getStart().getX()).append("_").append(tunnel.getStart().getY())
           .append("_").append(tunnel.getEnd().getX()).append("_").append(tunnel.getEnd().getY())
           .append("_").append(offsetX).append("_").append(offsetY)
           .append("_").append(String.format("%.2f", zoom));

        // Добавляем путь для уникальности
        for (PathPoint point : tunnel.getPath()) {
            key.append("_").append(point.getX()).append("_").append(point.getY());
        }

        return key.toString();
    }

    /**
     * Очистка кэша
     */
    public static void clearCache() {
        tunnelCache.clear();
        selectionCache.clear();
        controlPointCache.clear();
    }

    /**
     * Очистка кэша для конкретного туннеля
     */
    public static void clearCacheForTunnel(Tunnel tunnel) {
        String baseKey = createBaseCacheKey(tunnel, 0, 0, 1.0f);
        tunnelCache.keySet().removeIf(key -> key.contains(baseKey));
        selectionCache.keySet().removeIf(key -> key.contains(baseKey));
    }

    /**
     * Получение размера кэша
     */
    public static int getCacheSize() {
        return tunnelCache.size() + selectionCache.size() + controlPointCache.size();
    }
}