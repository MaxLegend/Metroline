package metroline.objects.gameobjects;

import metroline.core.world.World;
import metroline.core.world.tiles.WorldTile;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.TunnelType;
import metroline.screens.render.StationRender;
import metroline.screens.render.TunnelRender;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.MetroLogger;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

/**
 * Tunnel game object connecting stations
 * TODO Чистка кода. Обобщение, разделение, комментирование. Вынести рендер в отдельный класс
 */
public class Tunnel extends GameObject {
    private TunnelType type;
    private Station start;
    private Station end;
    private List<PathPoint> path = new ArrayList<>();
    private PathPoint pathPoint; // For single bend tunnels
    private Color tunnelColor;

    public Tunnel() {
        super(0, 0);
    }
    public Tunnel(World world, Station start, Station end, TunnelType type) {
        super(start.getX(), start.getY());
        this.setWorld(world);
        this.name = "tunnel_" + getUniqueId();
        this.start = start;
        this.end = end;
        this.type = type;
        calculatePath();
    }

    public void setTunnelColor(Color tunnelColor) {
        this.tunnelColor = tunnelColor;
    }
    public Color getTunnelColor() {
        return tunnelColor;
    }
    public void setType(TunnelType type) {
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

    public double getPathLenth() {
        return getPath().size();
    }
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
    public int getLength() {
        if (path.size() < 2) return 0;
        return path.size() - 1; // Количество сегментов между точками
    }
    private Shape createStrokedShape(List<PathPoint> path, int offsetX, int offsetY, float zoom, float width) {
        GeneralPath pathShape = new GeneralPath();

        // Начинаем с первой точки
        PathPoint first = path.get(0);
        int startX = (int)((first.getX() * 32 + offsetX + 16) * zoom);
        int startY = (int)((first.getY() * 32 + offsetY + 16) * zoom);
        pathShape.moveTo(startX, startY);

        // Добавляем промежуточные точки
        for (int i = 1; i < path.size(); i++) {
            PathPoint current = path.get(i);
            int x = (int)((current.getX() * 32 + offsetX + 16) * zoom);
            int y = (int)((current.getY() * 32 + offsetY + 16) * zoom);
            pathShape.lineTo(x, y);
        }

        // Создаем контур с заданной толщиной
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f).createStrokedShape(pathShape);
    }

@Override
public void draw(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
    TunnelRender.drawTunnel(this, g2d, offsetX, offsetY, zoom);

    if(getWorld().getWorldScreen() instanceof GameWorldScreen s) {
        if(s.debugMode) {
            // Дополнительно можно рисовать выделение и точку управления
            if (isSelected()) {
                TunnelRender.drawTunnelSelection(this, g2d, offsetX, offsetY, zoom);
            }

            if (getPathPoint() != null) {
                TunnelRender.drawControlPoint(this, g2d, offsetX, offsetY, zoom);
            }
        }
    }
}
    public PathPoint getPathPoint() {
        return pathPoint;
    }

    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }


}

