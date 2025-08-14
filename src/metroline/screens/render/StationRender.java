package metroline.screens.render;

import metroline.objects.enums.Direction;
import metroline.objects.enums.StationType;
import metroline.objects.gameobjects.Station;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.EnumMap;
import java.util.Map;


public class StationRender {


    /**************************
     * ROUND STATIONS SECTION
     **************************/
    public static void drawWorldColorRing(Station station,Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (24 * zoom);
        int cellCenterX = (int) ((station.getX() * 32 + offsetX + 16) * zoom);
        int cellCenterY = (int) ((station.getY() * 32 + offsetY + 16) * zoom);
        int drawX = cellCenterX - drawSize / 2;
        int drawY = cellCenterY - drawSize / 2;
        int holeSize = (int) (30 * zoom);
        int holeX = drawX + (drawSize - holeSize) / 2;
        int holeY = drawY + (drawSize - holeSize) / 2;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
        g2d.fillOval(holeX, holeY, holeSize, holeSize);
    }
    public static void drawRoundSelection(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (24 * zoom);
        int cellCenterX = (int) ((station.getX() * 32 + offsetX + 16) * zoom);
        int  cellCenterY = (int) ((station.getY() * 32 + offsetY + 16) * zoom);
        int  drawX = cellCenterX - drawSize / 2;
        int  drawY = cellCenterY - drawSize / 2;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.drawOval(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4);
    }

    public static void drawRoundTransfer(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int  cellCenterX = (int) ((station.getX() * 32 + offsetX + 16) * zoom);
        int  cellCenterY = (int) ((station.getY() * 32 + offsetY + 16) * zoom);
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
                    int nx = neighbor.getX() * 32 + offsetX + 16;
                    int ny = neighbor.getY() * 32 + offsetY + 16;

                    drawTransferConnection(g2d, cellCenterX, cellCenterY, nx, ny, dir, zoom);

                    // Градиентное соединение
                    Color[] colors = {station.getColor(), station.getColor(), neighbor.getColor(), neighbor.getColor()};
                    float[] fractions = {0f, 0.35f, 0.65f, 1f};

                    LinearGradientPaint gradient = new LinearGradientPaint(new Point2D.Float(cellCenterX, cellCenterY), new Point2D.Float(nx, ny), fractions, colors);
                    g2d.setPaint(gradient);
                    g2d.setStroke(new BasicStroke(connectionWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    drawTransferConnection(g2d, cellCenterX, cellCenterY, nx, ny, dir, zoom);
                }
            }
        }
    }


    public static void drawRoundStation(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int  drawSize = (int) (24 * zoom);
        int  cellCenterX = (int) ((station.getX() * 32 + offsetX + 16) * zoom);
        int  cellCenterY = (int) ((station.getY() * 32 + offsetY + 16) * zoom);
        int  drawX = cellCenterX - drawSize / 2;
        int   drawY = cellCenterY - drawSize / 2;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (station.getType() == StationType.PLANNED) {
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);

        }
        else if (station.getType() == StationType.BUILDING) {
            g2d.setColor(station.getColor());
            float[] dashPattern = {4 * zoom, 4 * zoom};
            g2d.setStroke(new BasicStroke(2 * zoom, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);
            g2d.setStroke(new BasicStroke(1 * zoom));

        }
        else if (station.getType() == StationType.RUINED) {

            g2d.setColor(station.getColor().darker().darker());
            g2d.setStroke(new BasicStroke(2.8f * zoom));

            int centerX = drawX + drawSize/2;
            int centerY = drawY + drawSize/2;
            int segment = drawSize/4;

            // Верхняя зигзаг-линия (3 излома)
            int[] xPoints1 = {
                    drawX + (int)(4*zoom),
                    centerX - segment/2,
                    centerX + segment/2,
                    drawX + 2 + drawSize - (int)(4*zoom)
            };
            int[] yPoints1 = {
                    drawY + (int)(6*zoom),
                    drawY + drawSize/3,
                    drawY + (int)(6*zoom),
                    drawY + 2 + drawSize/3
            };
            g2d.drawPolyline(xPoints1, yPoints1, 4);

            // Нижняя зигзаг-линия (3 излома)
            int[] xPoints2 = {
                    drawX + (int)(4*zoom),
                    centerX - segment/2,
                    centerX + segment/2,
                    drawX + 2 + drawSize - (int)(4*zoom)
            };
            int[] yPoints2 = {
                    drawY + drawSize - (int)(6*zoom),
                    drawY + drawSize - drawSize/3,
                    drawY + drawSize - (int)(6*zoom),
                    drawY + drawSize - drawSize/3
            };
            g2d.drawPolyline(xPoints2, yPoints2, 4);

            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);
        }
        else if (station.getType() == StationType.DESTROYED) {
            int padding = (int) (6 * zoom);
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.5f * zoom));

            int step = (int)(4 * zoom);
            for (int i = 0; i < 3; i++) {
                int offset = i * step - step;
                g2d.drawLine(drawX + padding + offset,
                        drawY + padding,
                        drawX + drawSize - padding + offset,
                        drawY + drawSize - padding);
            }
        }
        else if (station.getType() == StationType.ABANDONED) {

            int crossPadding = (int) (6 * zoom);
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);

            g2d.setStroke(new BasicStroke(3 * zoom));

            g2d.setColor(station.getColor().darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        }
        else if (station.getType() == StationType.BURNED) {

            int crossPadding = (int) (6 * zoom);
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);

            g2d.setStroke(new BasicStroke(3 * zoom));

            g2d.setColor(new Color(175, 67, 22, 216));
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        }
        else if (station.getType() == StationType.DROWNED) {

            int crossPadding = (int) (6 * zoom);
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);

            g2d.setStroke(new BasicStroke(3 * zoom));

            g2d.setColor(new Color(47, 73, 138, 216));
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        }
        else if (station.getType() == StationType.CLOSED) {
            int crossPadding = (int) (6 * zoom);
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);
            g2d.setColor(station.getColor().darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
        } else {
            g2d.setColor(station.getColor());
            g2d.fillOval(drawX, drawY, drawSize, drawSize);

        }
        if(station.isOnWater()) {
            if(station.getType() == StationType.PLANNED || station.getType() == StationType.BUILDING
                    || station.getType() == StationType.CLOSED || station.getType() == StationType.DESTROYED) {
                g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()).darker().darker());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.drawOval(drawX+4, drawY+4, drawSize-8, drawSize-8);
            } else {
                g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
                g2d.fillOval(drawX+5, drawY+5, drawSize-10, drawSize-10);

            }

        }
    }
    /**************************
     * TRANSFER STATIONS RENDER
     **************************/
    private static void drawTransferConnection(Graphics2D g2d, int x1, int y1, int x2, int y2,
            Direction dir, float zoom) {
        Path2D path = new Path2D.Float();
        path.moveTo(x1, y1);

        int ctrlX = (x1 + x2) / 2;
        int ctrlY = (y1 + y2) / 2;
        int curveIntensity = (int)(12 * zoom);

        switch (dir) {
            case NORTH:
                ctrlY -= curveIntensity;
                break;
            case NORTHEAST:
                ctrlX += curveIntensity;
                ctrlY -= curveIntensity;
                break;
            case EAST:
                ctrlX += curveIntensity;
                break;
            case SOUTHEAST:
                ctrlX += curveIntensity;
                ctrlY += curveIntensity;
                break;
            case SOUTH:
                ctrlY += curveIntensity;
                break;
            case SOUTHWEST:
                ctrlX -= curveIntensity;
                ctrlY += curveIntensity;
                break;
            case WEST:
                ctrlX -= curveIntensity;
                break;
            case NORTHWEST:
                ctrlX -= curveIntensity;
                ctrlY -= curveIntensity;
                break;
        }

        path.quadTo(ctrlX, ctrlY, x2, y2);
        g2d.draw(path);
    }


    /**************************
     * SQUARE STATION SECTIONS
     **************************/
    public static void drawWorldColorSquare(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(20 * zoom);
        int drawX = (int)((station.getX() * 32 + offsetX +6) * zoom);
        int drawY = (int)((station.getY() * 32 + offsetY +6) * zoom);
        int arcSize = (int)(drawSize * 0.35);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
        g2d.fillRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
    }

    public static void drawSquareSelection(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(20 * zoom);
        int drawX = (int)((station.getX() * 32 + offsetX +6) * zoom);
        int drawY = (int)((station.getY() * 32 + offsetY +6) * zoom);
        int arcSize = (int)(drawSize * 0.35);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.drawRoundRect(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4, arcSize, arcSize);
    }
    public static void drawSquareStation(Station station, Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(20 * zoom);
        int drawX = (int)((station.getX() * 32 + offsetX +6) * zoom);
        int drawY = (int)((station.getY() * 32 + offsetY +6) * zoom);
        int arcSize = (int)(drawSize * 0.35);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (station.getType() == StationType.PLANNED) {

            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

        }
        else if (station.getType() == StationType.BUILDING) {

            g2d.setColor(station.getColor());
            float[] dashPattern = {4 * zoom, 4 * zoom};
            g2d.setStroke(new BasicStroke(2 * zoom, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10, dashPattern, 0));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
            g2d.setStroke(new BasicStroke(1 * zoom));
        }
        else if (station.getType() == StationType.RUINED) {
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            // Настройки для зигзагов
            g2d.setColor(station.getColor().darker().darker());
            g2d.setStroke(new BasicStroke(2.8f * zoom));

            // Отступы от краев с учетом скругления
            int padding = (int)(6 * zoom);
            int innerPadding = (int)(4 * zoom); // Больший отступ для зигзагов

            // Центральные координаты
            int centerX = drawX + drawSize/2;
            int centerY = drawY + drawSize/2;

            // Верхняя зигзагообразная линия (3 излома)
            int[] xPointsTop = {
                    drawX + innerPadding,
                    centerX - (int)(drawSize * 0.15),
                    centerX + (int)(drawSize * 0.15),
                    drawX + drawSize - innerPadding
            };
            int[] yPointsTop = {
                    drawY + padding,
                    drawY + (int)(drawSize * 0.35),
                    drawY + padding,
                    drawY + (int)(drawSize * 0.35)
            };
            g2d.drawPolyline(xPointsTop, yPointsTop, 4);

            // Нижняя зигзагообразная линия (3 излома)
            int[] xPointsBottom = {
                    drawX + innerPadding,
                    centerX - (int)(drawSize * 0.15),
                    centerX + (int)(drawSize * 0.15),
                    drawX + drawSize - innerPadding
            };
            int[] yPointsBottom = {
                    drawY + drawSize - padding,
                    drawY + (int)(drawSize * 0.65),
                    drawY + drawSize - padding,
                    drawY + (int)(drawSize * 0.65)
            };
            g2d.drawPolyline(xPointsBottom, yPointsBottom, 4);

        }
        else if (station.getType() == StationType.BURNED) {
            int crossPadding = (int)(4 * zoom);

            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            g2d.setStroke(new BasicStroke(3 * zoom));

            g2d.setColor(new Color(175, 67, 22, 216));
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);

            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        }

        else if (station.getType() == StationType.DESTROYED) {
            // Разрушаемая станция - контур с крестом
            int crossPadding = (int)(4 * zoom);

            // Основной контур
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            // Крест внутри
            g2d.setStroke(new BasicStroke(3 * zoom));
            // Диагональ 1
            g2d.setColor(station.getColor().darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
            // Диагональ 2
            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        }else if (station.getType() == StationType.DROWNED) {

            int crossPadding = (int)(4 * zoom);

            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            g2d.setStroke(new BasicStroke(3 * zoom));

            g2d.setColor(new Color(47, 73, 138, 216));
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);

            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        }
        else if (station.getType() == StationType.CLOSED) {
            // Закрытая станция - контур с диагональной линией
            int crossPadding = (int)(4 * zoom);
            g2d.setColor(station.getColor());
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            // Диагональная линия
            g2d.setColor(station.getColor().darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
        } else {
            // Обычная отрисовка для других типов
            g2d.setColor(station.getColor());
            g2d.fillRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
        }
        if(station.isOnWater()) {
            if(station.getType() == StationType.PLANNED || station.getType() == StationType.BUILDING
                    || station.getType() == StationType.CLOSED || station.getType() == StationType.DESTROYED) {
                g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()).darker().darker());
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.drawRoundRect(drawX+3, drawY+3, drawSize-6, drawSize-6, arcSize, arcSize);
            } else {
                g2d.setColor(station.getWorld().getWorldColorAt(station.getX(), station.getY()));
                g2d.fillRoundRect(drawX+3, drawY+3, drawSize-6, drawSize-6, arcSize, arcSize);

            }

        }
    }


}
