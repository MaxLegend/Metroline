package metroline.screens.render;

import metroline.objects.enums.Direction;
import metroline.objects.enums.StationType;
import metroline.objects.gameobjects.Station;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StationRender {

    private static final Map<String, CachedStation> vectorCache = new ConcurrentHashMap<>();
    private static final int BASE_SIZE = 24;

    private static class CachedStation {
        public final Shape stationShape;
        public final Shape waterShape;
        public final Shape waterOutlineShape;
        public final Shape selectionShape;
        public final Shape worldColorShape;
        public final Map<StationType, Shape> specialShapes;

        public CachedStation(Shape stationShape, Shape waterShape, Shape waterOutlineShape,
                Shape selectionShape, Shape worldColorShape,
                Map<StationType, Shape> specialShapes) {
            this.stationShape = stationShape;
            this.waterShape = waterShape;
            this.waterOutlineShape = waterOutlineShape;
            this.selectionShape = selectionShape;
            this.worldColorShape = worldColorShape;
            this.specialShapes = specialShapes;
        }
    }

    /**************************
     * ROUND STATIONS SECTION
     **************************/

    public static void drawWorldColorRing(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        String cacheKey = "world_" + station.getX() + "_" + station.getY() + "_" + String.format("%.2f", zoom);
        CachedStation cached = vectorCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedStation(station, offsetX, offsetY, zoom);
            vectorCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
        g2d.fill(cached.worldColorShape);
    }

    public static void drawRoundSelection(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        String cacheKey = "selection_" + station.getX() + "_" + station.getY() + "_" + String.format("%.2f", zoom);
        CachedStation cached = vectorCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedStation(station, offsetX, offsetY, zoom);
            vectorCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.draw(cached.selectionShape);
    }

    public static void drawRoundTransfer(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int cellCenterX = (int) ((station.getX() * 32 + offsetX + 16) * zoom);
        int cellCenterY = (int) ((station.getY() * 32 + offsetY + 16) * zoom);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Map<Direction, Station> adjacentStations = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            int nx = station.getX() + dir.getDx();
            int ny = station.getY() + dir.getDy();
            Station neighbor = station.getWorld().getStationAt(nx, ny);
            if (neighbor != null && neighbor != station) {
                adjacentStations.put(dir, neighbor);
            }
        }

        if (station.getType() == StationType.TRANSFER && !adjacentStations.isEmpty()) {
            float connectionWidth = 14.0f * zoom;

            for (Map.Entry<Direction, Station> entry : adjacentStations.entrySet()) {
                Direction dir = entry.getKey();
                Station neighbor = entry.getValue();

                if (!neighbor.getColor().equals(station.getColor())) {
                    int nx = (int) ((neighbor.getX() * 32 + offsetX + 16) * zoom);
                    int ny = (int) ((neighbor.getY() * 32 + offsetY + 16) * zoom);

                    // Градиентное соединение
                    Color[] colors = {station.getColor(), station.getColor(), neighbor.getColor(), neighbor.getColor()};
                    float[] fractions = {0f, 0.35f, 0.65f, 1f};

                    LinearGradientPaint gradient = new LinearGradientPaint(
                            new Point2D.Float(cellCenterX, cellCenterY),
                            new Point2D.Float(nx, ny),
                            fractions, colors
                    );

                    g2d.setPaint(gradient);
                    g2d.setStroke(new BasicStroke(connectionWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    // Создаем и рисуем путь соединения
                    Path2D path = createTransferPath(cellCenterX, cellCenterY, nx, ny, dir, zoom);
                    g2d.draw(path);
                }
            }
        }
    }

    public static void drawRoundStation(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        String cacheKey = "station_" + station.getType() + "_" + station.getX() + "_" +
                station.getY() + "_" + String.format("%.2f", zoom);
        CachedStation cached = vectorCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedStation(station, offsetX, offsetY, zoom);
            vectorCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Отрисовка основной станции
        switch (station.getType()) {
            case DEPO:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(5 * zoom));
                g2d.draw(cached.stationShape);
                break;

            case ABANDONED:
            case REPAIR:
            case RUINED:
            case DESTROYED:
            case BURNED:
            case DROWNED:
            case CLOSED:
            case PLANNED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                break;

            case BUILDING:
                g2d.setColor(station.getColor());
                float[] dashPattern = {4 * zoom, 4 * zoom};
                g2d.setStroke(new BasicStroke(2 * zoom, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10, dashPattern, 0));
                g2d.draw(cached.stationShape);
                g2d.setStroke(new BasicStroke(1 * zoom));
                break;

            default:
                g2d.setColor(station.getColor());
                g2d.fill(cached.stationShape);
        }

        // Отрисовка специальных элементов
        if (cached.specialShapes.containsKey(station.getType())) {
            drawSpecialElements(station, g2d, cached, zoom);
        }
        boolean onWaterTile = station.isOnWater();
        boolean onRiver = station.isOnRiver();
        // Водный эффект
        if (onWaterTile || onRiver) {
            drawWaterEffect(station, g2d, cached, zoom);
        }
    }

    /**************************
     * SQUARE STATIONS SECTION
     **************************/

    public static void drawWorldColorSquare(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        String cacheKey = "square_world_" + station.getX() + "_" + station.getY() + "_" + String.format("%.2f", zoom);
        CachedStation cached = vectorCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedSquareStation(station, offsetX, offsetY, zoom);
            vectorCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
        g2d.fill(cached.worldColorShape);
    }

    public static void drawSquareSelection(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        String cacheKey = "square_selection_" + station.getX() + "_" + station.getY() + "_" + String.format("%.2f", zoom);
        CachedStation cached = vectorCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedSquareStation(station, offsetX, offsetY, zoom);
            vectorCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.draw(cached.selectionShape);
    }

    public static void drawSquareStation(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        String cacheKey = "square_" + station.getType() + "_" + station.getX() + "_" +
                station.getY() + "_" + String.format("%.2f", zoom);
        CachedStation cached = vectorCache.get(cacheKey);

        if (cached == null) {
            cached = createCachedSquareStation(station, offsetX, offsetY, zoom);
            vectorCache.put(cacheKey, cached);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Отрисовка основной станции
        switch (station.getType()) {
            case DEPO:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(5 * zoom));
                g2d.draw(cached.stationShape);
                break;

            case ABANDONED:
            case REPAIR:
            case RUINED:
            case DESTROYED:
            case BURNED:
            case DROWNED:
            case CLOSED:
            case PLANNED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                break;


            case BUILDING:
                g2d.setColor(station.getColor());
                float[] dashPattern = {4 * zoom, 4 * zoom};
                g2d.setStroke(new BasicStroke(2 * zoom, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10, dashPattern, 0));
                g2d.draw(cached.stationShape);
                g2d.setStroke(new BasicStroke(1 * zoom));
                break;

            default:
                g2d.setColor(station.getColor());
                g2d.fill(cached.stationShape);
        }

        // Отрисовка специальных элементов
        if (cached.specialShapes.containsKey(station.getType())) {
            drawSpecialSquareElements(station, g2d, cached, zoom);
        }

        // Водный эффект
        if (station.isOnWater()) {
            drawSquareWaterEffect(station, g2d, cached, zoom);
        }
    }
    private static void createSpecialSquareShapes(Station station, Map<StationType, Shape> specialShapes,
            int drawX, int drawY, int drawSize, int arcSize, float zoom) {

        int centerX = drawX + drawSize / 2;
        int centerY = drawY + drawSize / 2;

        switch (station.getType()) {
            case RUINED:
                // Зигзагообразные линии для квадратной разрушенной станции
                Path2D zigzagPath = new Path2D.Float();
                int padding = (int)(6 * zoom);
                int innerPadding = (int)(4 * zoom);

                // Верхняя зигзагообразная линия
                zigzagPath.moveTo(drawX + innerPadding, drawY + padding);
                zigzagPath.lineTo(centerX - (int)(drawSize * 0.15), drawY + (int)(drawSize * 0.35));
                zigzagPath.lineTo(centerX + (int)(drawSize * 0.15), drawY + padding);
                zigzagPath.lineTo(drawX + drawSize - innerPadding, drawY + (int)(drawSize * 0.35));

                // Нижняя зигзагообразная линия
                zigzagPath.moveTo(drawX + innerPadding, drawY + drawSize - padding);
                zigzagPath.lineTo(centerX - (int)(drawSize * 0.15), drawY + (int)(drawSize * 0.65));
                zigzagPath.lineTo(centerX + (int)(drawSize * 0.15), drawY + drawSize - padding);
                zigzagPath.lineTo(drawX + drawSize - innerPadding, drawY + (int)(drawSize * 0.65));

                specialShapes.put(StationType.RUINED, zigzagPath);
                break;
            case REPAIR:
                // Warning triangle icon for square station
                Path2D trianglePath = new Path2D.Float();
                int trianglePadding = (int) (5 * zoom);
                int triangleTop = drawY + trianglePadding;
                int triangleBottom = drawY + drawSize - trianglePadding;
                int triangleLeft = drawX + trianglePadding;
                int triangleRight = drawX + drawSize - trianglePadding;

                // Create upward-pointing triangle
                trianglePath.moveTo(centerX, triangleTop); // Top point
                trianglePath.lineTo(triangleLeft, triangleBottom); // Bottom left
                trianglePath.lineTo(triangleRight, triangleBottom); // Bottom right
                trianglePath.closePath();

                specialShapes.put(StationType.REPAIR, trianglePath);
                break;
            case DESTROYED:
            case ABANDONED:
            case BURNED:
            case DROWNED:
            case CLOSED:
                // Кресты и диагональные линии для квадратных станций
                Path2D crossPath = new Path2D.Float();
                int crossPadding = (int)(4 * zoom);

                // Основная диагональ
                crossPath.moveTo(drawX + crossPadding, drawY + crossPadding);
                crossPath.lineTo(drawX + drawSize - crossPadding, drawY + drawSize - crossPadding);

                // Вторая диагональ (кроме CLOSED)
                if (station.getType() != StationType.CLOSED) {
                    crossPath.moveTo(drawX + drawSize - crossPadding, drawY + crossPadding);
                    crossPath.lineTo(drawX + crossPadding, drawY + drawSize - crossPadding);
                }

                specialShapes.put(station.getType(), crossPath);
                break;
        }
    }

    private static void drawSpecialSquareElements(Station station, Graphics2D g2d, CachedStation cached, float zoom) {
        Shape specialShape = cached.specialShapes.get(station.getType());

        switch (station.getType()) {
            case RUINED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                g2d.setColor(station.getColor().darker().darker());
                g2d.setStroke(new BasicStroke(2.8f * zoom));
                g2d.draw(specialShape);
                break;

            case DESTROYED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                g2d.setColor(station.getColor().darker().darker());
                g2d.setStroke(new BasicStroke(3 * zoom));
                g2d.draw(specialShape);
                break;

            case ABANDONED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                g2d.setStroke(new BasicStroke(3 * zoom));
                g2d.setColor(station.getColor().darker().darker());
                g2d.draw(specialShape);
                break;

            case BURNED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                g2d.setStroke(new BasicStroke(3 * zoom));
                g2d.setColor(new Color(175, 67, 22, 216));
                g2d.draw(specialShape);
                break;

            case DROWNED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                g2d.setStroke(new BasicStroke(3 * zoom));
                g2d.setColor(new Color(47, 73, 138, 216));
                g2d.draw(specialShape);
                break;

            case CLOSED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                g2d.setColor(station.getColor().darker().darker());
                g2d.draw(specialShape);
                break;
            case REPAIR:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                // Fill triangle icon
                g2d.setColor(station.getColor().darker());
                g2d.fill(specialShape);
                break;
        }
    }

    private static void drawSquareWaterEffect(Station station, Graphics2D g2d, CachedStation cached, float zoom) {
        if (station.getType() == StationType.PLANNED || station.getType() == StationType.BUILDING ||
                station.getType() == StationType.CLOSED || station.getType() == StationType.DESTROYED) {
            g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()).darker().darker());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.draw(cached.waterOutlineShape);
        } else {
            g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
            g2d.fill(cached.waterShape);
        }
    }
    /**************************
     * PRIVATE HELPER METHODS
     **************************/

    private static CachedStation createCachedStation(Station station, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (BASE_SIZE * zoom);
        int cellCenterX = (int) ((station.getX() * 32 + offsetX + 16) * zoom);
        int cellCenterY = (int) ((station.getY() * 32 + offsetY + 16) * zoom);
        int drawX = cellCenterX - drawSize / 2;
        int drawY = cellCenterY - drawSize / 2;

        // Основные формы
        Shape stationShape = new Ellipse2D.Float(drawX, drawY, drawSize, drawSize);
        Shape waterShape = new Ellipse2D.Float(drawX + 5 * zoom, drawY + 5 * zoom,
                drawSize - 10 * zoom, drawSize - 10 * zoom);
        Shape waterOutlineShape = new Ellipse2D.Float(drawX + 4 * zoom, drawY + 4 * zoom,
                drawSize - 8 * zoom, drawSize - 8 * zoom);
        Shape selectionShape = new Ellipse2D.Float(drawX - 2 * zoom, drawY - 2 * zoom,
                drawSize + 4 * zoom, drawSize + 4 * zoom);

        int holeSize = (int) (30 * zoom);
        int holeX = cellCenterX - holeSize / 2;
        int holeY = cellCenterY - holeSize / 2;
        Shape worldColorShape = new Ellipse2D.Float(holeX, holeY, holeSize, holeSize);

        // Специальные формы для разных типов станций
        Map<StationType, Shape> specialShapes = new EnumMap<>(StationType.class);
        createSpecialShapes(station, specialShapes, drawX, drawY, drawSize, zoom);

        return new CachedStation(stationShape, waterShape, waterOutlineShape,
                selectionShape, worldColorShape, specialShapes);
    }

    private static CachedStation createCachedSquareStation(Station station, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (20 * zoom);
        int drawX = (int) ((station.getX() * 32 + offsetX + 6) * zoom);
        int drawY = (int) ((station.getY() * 32 + offsetY + 6) * zoom);
        int arcSize = (int) (drawSize * 0.35);

        // Основные формы
        Shape stationShape = new RoundRectangle2D.Float(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
        Shape waterShape = new RoundRectangle2D.Float(drawX + 3 * zoom, drawY + 3 * zoom,
                drawSize - 6 * zoom, drawSize - 6 * zoom, arcSize, arcSize);
        Shape waterOutlineShape = new RoundRectangle2D.Float(drawX + 2 * zoom, drawY + 2 * zoom,
                drawSize - 4 * zoom, drawSize - 4 * zoom, arcSize, arcSize);
        Shape selectionShape = new RoundRectangle2D.Float(drawX - 2 * zoom, drawY - 2 * zoom,
                drawSize + 4 * zoom, drawSize + 4 * zoom, arcSize, arcSize);

        Shape worldColorShape = new RoundRectangle2D.Float(drawX-4, drawY-4, drawSize+8, drawSize+8, arcSize, arcSize);

        // Специальные формы
        Map<StationType, Shape> specialShapes = new EnumMap<>(StationType.class);
        createSpecialSquareShapes(station, specialShapes, drawX, drawY, drawSize, arcSize, zoom);

        return new CachedStation(stationShape, waterShape, waterOutlineShape,
                selectionShape, worldColorShape, specialShapes);
    }

    private static void createSpecialShapes(Station station, Map<StationType, Shape> specialShapes,
            int drawX, int drawY, int drawSize, float zoom) {
        int centerX = drawX + drawSize / 2;
        int centerY = drawY + drawSize / 2;

        switch (station.getType()) {
            case RUINED:
                // Зигзагообразные линии для разрушенной станции
                Path2D zigzagPath = new Path2D.Float();
                int segment = drawSize / 4;

                // Верхняя зигзаг-линия
                zigzagPath.moveTo(drawX + 4 * zoom, drawY + 6 * zoom);
                zigzagPath.lineTo(centerX - segment / 2, drawY + drawSize / 3);
                zigzagPath.lineTo(centerX + segment / 2, drawY + 6 * zoom);
                zigzagPath.lineTo(drawX + drawSize - 4 * zoom, drawY + drawSize / 3);

                // Нижняя зигзаг-линия
                zigzagPath.moveTo(drawX + 4 * zoom, drawY + drawSize - 6 * zoom);
                zigzagPath.lineTo(centerX - segment / 2, drawY + drawSize - drawSize / 3);
                zigzagPath.lineTo(centerX + segment / 2, drawY + drawSize - 6 * zoom);
                zigzagPath.lineTo(drawX + drawSize - 4 * zoom, drawY + drawSize - drawSize / 3);

                specialShapes.put(StationType.RUINED, zigzagPath);
                break;
            case REPAIR:
                // Warning triangle icon
                Path2D trianglePath = new Path2D.Float();
                int trianglePadding = (int) (7 * zoom);
                int triangleTop = drawY + trianglePadding;
                int triangleBottom = drawY + drawSize - trianglePadding;
                int triangleLeft = drawX + trianglePadding;
                int triangleRight = drawX + drawSize - trianglePadding;
                int triangleCenterX = centerX;

                // Create upward-pointing triangle
                trianglePath.moveTo(triangleCenterX, triangleTop); // Top point
                trianglePath.lineTo(triangleLeft, triangleBottom); // Bottom left
                trianglePath.lineTo(triangleRight, triangleBottom); // Bottom right
                trianglePath.closePath();

                specialShapes.put(StationType.REPAIR, trianglePath);
                break;
            case DESTROYED:
                // Линии разрушения
                Path2D destructionPath = new Path2D.Float();
                int padding = (int) (6 * zoom);

                for (int i = 0; i < 3; i++) {
                    int offset = i * (int)(4 * zoom) - (int)(4 * zoom);
                    destructionPath.moveTo(drawX + padding + offset, drawY + padding);
                    destructionPath.lineTo(drawX + drawSize - padding + offset, drawY + drawSize - padding);
                }

                specialShapes.put(StationType.DESTROYED, destructionPath);
                break;

            case ABANDONED:
            case BURNED:
            case DROWNED:
            case CLOSED:
                // Кресты и диагональные линии
                Path2D crossPath = new Path2D.Float();
                int crossPadding = (int) (6 * zoom);

                crossPath.moveTo(drawX + crossPadding, drawY + crossPadding);
                crossPath.lineTo(drawX + drawSize - crossPadding, drawY + drawSize - crossPadding);

                if (station.getType() != StationType.CLOSED) {
                    crossPath.moveTo(drawX + drawSize - crossPadding, drawY + crossPadding);
                    crossPath.lineTo(drawX + crossPadding, drawY + drawSize - crossPadding);
                }

                specialShapes.put(station.getType(), crossPath);
                break;
        }
    }



    private static void drawSpecialElements(Station station, Graphics2D g2d, CachedStation cached, float zoom) {
        Shape specialShape = cached.specialShapes.get(station.getType());

        switch (station.getType()) {
            case RUINED:
                g2d.setColor(station.getColor().darker().darker());
                g2d.setStroke(new BasicStroke(2.8f * zoom));
                g2d.draw(specialShape);
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                break;

            case DESTROYED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.5f * zoom));
                g2d.draw(specialShape);
                break;

            case ABANDONED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                g2d.setStroke(new BasicStroke(3 * zoom));
                g2d.setColor(station.getColor().darker().darker());
                g2d.draw(specialShape);
                break;

            case BURNED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                g2d.setStroke(new BasicStroke(3 * zoom));
                g2d.setColor(new Color(175, 67, 22, 216));
                g2d.draw(specialShape);
                break;

            case DROWNED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                g2d.setStroke(new BasicStroke(3 * zoom));
                g2d.setColor(new Color(47, 73, 138, 216));
                g2d.draw(specialShape);
                break;

            case CLOSED:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);
                g2d.setColor(station.getColor().darker().darker());
                g2d.draw(specialShape);
                break;

            case REPAIR:
                g2d.setColor(station.getColor());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.draw(cached.stationShape);

                g2d.setColor(station.getColor().darker());
                g2d.fill(specialShape);
                break;
        }
    }

    private static void drawWaterEffect(Station station, Graphics2D g2d, CachedStation cached, float zoom) {
        if (station.getType() == StationType.PLANNED || station.getType() == StationType.BUILDING ||
                station.getType() == StationType.CLOSED || station.getType() == StationType.DESTROYED) {
            g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()).darker().darker());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.draw(cached.waterOutlineShape);
        } else {
            g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
            g2d.fill(cached.waterShape);
        }
    }



    private static Path2D createTransferPath(int x1, int y1, int x2, int y2, Direction dir, float zoom) {
        Path2D path = new Path2D.Float();
        path.moveTo(x1, y1);

        int ctrlX = (x1 + x2) / 2;
        int ctrlY = (y1 + y2) / 2;
        int curveIntensity = (int)(12 * zoom);

        switch (dir) {
            case NORTH: ctrlY -= curveIntensity; break;
            case NORTHEAST: ctrlX += curveIntensity; ctrlY -= curveIntensity; break;
            case EAST: ctrlX += curveIntensity; break;
            case SOUTHEAST: ctrlX += curveIntensity; ctrlY += curveIntensity; break;
            case SOUTH: ctrlY += curveIntensity; break;
            case SOUTHWEST: ctrlX -= curveIntensity; ctrlY += curveIntensity; break;
            case WEST: ctrlX -= curveIntensity; break;
            case NORTHWEST: ctrlX -= curveIntensity; ctrlY -= curveIntensity; break;
        }

        path.quadTo(ctrlX, ctrlY, x2, y2);
        return path;
    }

    /**************************
     * UTILITY METHODS
     **************************/

    public static void clearCache() {
        vectorCache.clear();
    }

    public static void clearCacheForStation(Station station) {
        // Удаляем все кэшированные данные для конкретной станции
        vectorCache.keySet().removeIf(key -> key.contains(station.getX() + "_" + station.getY()));
    }

    public static int getCacheSize() {
        return vectorCache.size();
    }
}

