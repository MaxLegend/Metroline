package game.objects;

import game.core.GameObject;
import game.core.GameTime;
import game.core.world.World;
import game.objects.enums.TunnelType;
import screens.WorldSandboxScreen;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Tunnel game object connecting stations
 */
public class Tunnel extends GameObject {
    private TunnelType type;
    private Station start;
    private Station end;
    private List<PathPoint> path = new ArrayList<>();
    private PathPoint pathPoint; // For single bend tunnels
    private long buildStartTime;
    private long buildDuration = 8 * 60 * 1000; // 8 минут игрового времени

    public Tunnel() {
        super(0, 0);
    }
    public Tunnel(World world, Station start, Station end, TunnelType type) {
        super(start.getX(), start.getY());
        this.setWorld(world);
        this.start = start;
        this.end = end;
        this.type = type;
        calculatePath();
    }
    public void setBuildStartTime(long gameTime) {
        this.buildStartTime = gameTime;
    }

    public void setBuildDuration(long duration) {
        this.buildDuration = duration;
    }

    public boolean isConstructionComplete(long currentGameTime) {
        if (type != TunnelType.BUILDING) return false;
        return currentGameTime - buildStartTime >= buildDuration;
    }

    public float getConstructionProgress(long currentGameTime) {
        if (type != TunnelType.BUILDING) return 1.0f;
        return Math.min(1.0f, (float)(currentGameTime - buildStartTime) / buildDuration);
    }

    public void setType(TunnelType type, GameTime gameTime) {
        if (type == TunnelType.BUILDING && this.type != TunnelType.BUILDING) {
            this.buildStartTime = gameTime.getCurrentTimeMillis();
        }
        this.type = type;
    }

    public TunnelType getType() {
        return type;
    }

    /**
     * Gets the start station
     * @return Start station
     */
    public Station getStart() { return start; }

    /**
     * Gets the end station
     * @return End station
     */
    public Station getEnd() { return end; }

    /**
     * Gets the path PathPoints
     * @return List of path PathPoints
     */
    public List<PathPoint> getPath() { return path; }

    /**
     * Recalculates the path between stations with maximum one bend
     */
    public void calculatePath() {
        path.clear();

        int x1 = start.getX();
        int y1 = start.getY();
        int x2 = end.getX();
        int y2 = end.getY();


        // Если нет точки изгиба, выбираем оптимальную
        if (pathPoint == null) {
            pathPoint = findOptimalBendPoint(x1, y1, x2, y2);
        }

        // Строим путь через точку изгиба
        addBendPath(x1, y1, pathPoint.getX(), pathPoint.getY(), x2, y2);
    }

    /**
     * Finds optimal bend point considering diagonal paths
     */
    private PathPoint findOptimalBendPoint(int x1, int y1, int x2, int y2) {
        PathPoint bend1 = new PathPoint(x1, y2);
        PathPoint bend2 = new PathPoint(x2, y1);
        PathPoint bend3 = findDiagonalBendPoint(x1, y1, x2, y2);

        double length1 = calculatePathLength(x1, y1, bend1.getX(), bend1.getY(), x2, y2);
        double length2 = calculatePathLength(x1, y1, bend2.getX(), bend2.getY(), x2, y2);
        double length3 = bend3 != null ?
                calculatePathLength(x1, y1, bend3.getX(), bend3.getY(), x2, y2) : Double.MAX_VALUE;

        if (length3 <= length1 && length3 <= length2 && bend3 != null) {
            return bend3;
        } else if (length1 <= length2) {
            return bend1;
        } else {
            return bend2;
        }
    }

    /**
     * Calculates total path length through bend point
     */
    private double calculatePathLength(int x1, int y1, int bx, int by, int x2, int y2) {
        double firstSegment = Math.sqrt(Math.pow(bx - x1, 2) + Math.pow(by - y1, 2));
        double secondSegment = Math.sqrt(Math.pow(x2 - bx, 2) + Math.pow(y2 - by, 2));
        return firstSegment + secondSegment;
    }

    /**
     * Finds a diagonal bend point
     */
    private PathPoint findDiagonalBendPoint(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;

        // Пробуем несколько вариантов диагональных точек
        PathPoint[] diagonalOptions = {
                new PathPoint(x1 + dx/2, y1 + dy/2),  // Середина
                new PathPoint(x1 + dx/3, y1 + dy*2/3), // 1/3 по X, 2/3 по Y
                new PathPoint(x1 + dx*2/3, y1 + dy/3)  // 2/3 по X, 1/3 по Y
        };

        // Ищем первую точку, которая дает допустимый угол
        for (PathPoint point : diagonalOptions) {
            if (isValidAngle(x1, y1, point.getX(), point.getY(), x2, y2)) {
                return point;
            }
        }
        return null;
    }


    /**
     * Checks if the angle formed by three points is >= 90 degrees
     */
    private boolean isValidAngle(int x1, int y1, int bx, int by, int x2, int y2) {
        // Vectors from bend point to start and end
        int v1x = x1 - bx;
        int v1y = y1 - by;
        int v2x = x2 - bx;
        int v2y = y2 - by;

        // Dot product
        int dotProduct = v1x * v2x + v1y * v2y;

        // If dot product is <= 0, the angle is >= 90 degrees
        return dotProduct <= 0;
    }

    private void addStraightPath(int x1, int y1, int x2, int y2) {
        // Use Bresenham's line algorithm to get all points between start and end
        List<Point> points = getLinePoints(x1, y1, x2, y2);
        for (Point p : points) {
            path.add(new PathPoint(p.x, p.y));
        }
    }

    private void addBendPath(int x1, int y1, int cx, int cy, int x2, int y2) {
        // First segment to bend point
        addStraightPath(x1, y1, cx, cy);

        // Remove duplicate bend point
        if (!path.isEmpty()) {
            path.remove(path.size() - 1);
        }

        // Second segment from bend point to end
        addStraightPath(cx, cy, x2, y2);
    }

    /**
     * Bresenham's line algorithm to get all points between two points
     */
    private List<Point> getLinePoints(int x1, int y1, int x2, int y2) {
        List<Point> points = new ArrayList<>();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;
        int currentX = x1;
        int currentY = y1;

        while (true) {
            points.add(new Point(currentX, currentY));

            if (currentX == x2 && currentY == y2) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currentX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }

        return points;
    }
    public void moveControlPoint(int x, int y) {
        if (pathPoint.getX() != x || pathPoint.getY() != y) {
            this.pathPoint = new PathPoint(x, y);
            calculatePath();
        }
    }
    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        if (path.size() < 2) return;

        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(start.getColor());

        // Общие настройки для всех типов туннелей
        float baseWidth = 12 * zoom; // Базовая толщина

        if (type == TunnelType.PLANNED) {
            // Планируемый туннель - двойная линия (контур)
            float innerWidth = baseWidth - 4 * zoom;
            g2d.setStroke(new BasicStroke(baseWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawTunnelPath(g2d, offsetX, offsetY, zoom);

            // Рисуем внутреннюю линию цветом фона
            g2d.setColor(getWorld().getWorldColorAt(start.getX(), start.getY()));
            g2d.setStroke(new BasicStroke(innerWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawTunnelPath(g2d, offsetX, offsetY, zoom);

        } else if (type == TunnelType.BUILDING) {
            float innerWidth = baseWidth - 4 * zoom;
            float dashLength = 4.0f * zoom; // длина штриха
            float gapLength = 22.0f * zoom;   // длина пробела

            g2d.setStroke(new BasicStroke(
                    innerWidth,
                    BasicStroke.CAP_SQUARE,
                    BasicStroke.JOIN_ROUND,
                    0,
                    new float[]{dashLength, gapLength}, // паттерн штриховки
                    0
            ));
            drawTunnelPath(g2d, offsetX, offsetY, zoom);

        } else {
            // Активный туннель - обычная сплошная линия
            g2d.setStroke(new BasicStroke(
                    baseWidth,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));
            drawTunnelPath(g2d, offsetX, offsetY, zoom);
        }

        // Отрисовка контрольных точек в debug-режиме
        if(WorldSandboxScreen.getInstance().debugMode) {
            if (selected || pathPoint != null) {
                g2d.setColor(Color.LIGHT_GRAY);
                for (PathPoint p : path) {
                    int x = (int) ((p.getX() * 32 + offsetX + 16) * zoom);
                    int y = (int) ((p.getY() * 32 + offsetY + 16) * zoom);
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        }
    }


    private void drawTunnelPath(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        GeneralPath tunnelPath = new GeneralPath();

        PathPoint first = path.get(0);
        int startX = (int)((first.getX() * 32 + offsetX + 15) * zoom);
        int startY = (int)((first.getY() * 32 + offsetY + 15) * zoom);
        tunnelPath.moveTo(startX, startY);

        for (int i = 1; i < path.size(); i++) {
            PathPoint current = path.get(i);
            int x = (int)((current.getX() * 32 + offsetX + 15) * zoom);
            int y = (int)((current.getY() * 32 + offsetY + 15) * zoom);
            tunnelPath.lineTo(x, y);
        }

        g2d.draw(tunnelPath);
    }
}

