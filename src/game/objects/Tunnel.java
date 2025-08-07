package game.objects;

import game.core.GameObject;
import screens.WorldSandboxScreen;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

/**
 * Tunnel game object connecting stations
 */
public class Tunnel extends GameObject {

    private Station start;
    private Station end;
    private List<PathPoint> path = new ArrayList<>();
    private PathPoint pathPoint; // For single bend tunnels

    public Tunnel() {
        super(0, 0);
    }
    public Tunnel(Station start, Station end) {
        super(start.getX(), start.getY());
        this.start = start;
        this.end = end;
        calculatePath();
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

    /**
     * Finds a valid bend point that creates angle >= 90 degrees
     */
    private PathPoint findValidBendPoint(int x1, int y1, int x2, int y2) {
        // Try points along the perpendicular bisector
        int midX = (x1 + x2) / 4;
        int midY = (y1 + y2) / 4;

        // Try different points around the midpoint
        for (int i = 0; i < 4; i++) {
            int bx, by;
            switch (i) {
                case 0: bx = midX + (y2 - y1); by = midY - (x2 - x1); break;
                case 1: bx = midX - (y2 - y1); by = midY + (x2 - x1); break;
                case 2: bx = midX + (y2 - y1)/2; by = midY - (x2 - x1)/2; break;
                case 3: bx = midX - (y2 - y1)/2; by = midY + (x2 - x1)/2; break;
                default: bx = midX; by = midY;
            }

            if (isValidAngle(x1, y1, bx, by, x2, y2)) {
                return new PathPoint(bx, by);
            }
        }

        // Fallback to midpoint if no other point found (though angle might be <90)
        return new PathPoint(midX, midY);
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
        g2d.setStroke(new BasicStroke(12 * zoom, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Создаем путь для плавного соединения сегментов
        GeneralPath tunnelPath = new GeneralPath();

        PathPoint first = path.get(0);
        int startX = (int)((first.getX() * 32 + offsetX + 16) * zoom);
        int startY = (int)((first.getY() * 32 + offsetY + 16) * zoom);
        tunnelPath.moveTo(startX, startY);

        for (int i = 1; i < path.size(); i++) {
            PathPoint current = path.get(i);
            int x = (int)((current.getX() * 32 + offsetX + 16) * zoom);
            int y = (int)((current.getY() * 32 + offsetY + 16) * zoom);
            tunnelPath.lineTo(x, y);
        }

        g2d.draw(tunnelPath);
        if(WorldSandboxScreen.getInstance().debugMode) {
            // Отрисовка контрольных точек
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

}

