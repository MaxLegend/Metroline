package metroline.objects.gameobjects;

import metroline.core.world.World;
import metroline.input.selection.Selectable;
import metroline.input.selection.SelectionManager;
import metroline.screens.render.RiverRender;
import metroline.screens.worldscreens.normal.GameWorldScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * River - contains multiple RiverPoint objects, renders path through them.
 * RiverPoints are stored in gameGrid, River just manages the connection.
 */
public class River extends GameObject {

    private List<RiverPoint> points = new ArrayList<>();
    private List<PathPoint> calculatedPath = new ArrayList<>(); // Interpolated render path
    private Color riverColor = new Color(70, 130, 180, 200);
    private float width = 20f;

    public River() {
        super(0, 0);
        this.name = "river_" + getUniqueId();
    }

    public River(World world) {
        super(0, 0);
        this.setWorld(world);
        this.name = "river_" + getUniqueId();
    }

    public List<RiverPoint> getPoints() {
        return points;
    }

    public List<PathPoint> getCalculatedPath() {
        return calculatedPath;
    }

    public Color getRiverColor() {
        return riverColor;
    }

    public void setRiverColor(Color color) {
        this.riverColor = color;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    /**
     * Add a new control point to the river
     */
    public void addPoint(RiverPoint point) {
        point.setParentRiver(this);
        point.setOrderIndex(points.size());
        points.add(point);

        // Update river position to first point
        if (points.size() == 1) {
            this.x = point.getX();
            this.y = point.getY();
        }

        recalculatePath();
        RiverRender.clearCacheForRiver(this);
    }

    /**
     * Insert point at specific index
     */
    public void insertPoint(int index, RiverPoint point) {
        point.setParentRiver(this);
        points.add(index, point);
        updatePointIndices();
        recalculatePath();
        RiverRender.clearCacheForRiver(this);
    }

    /**
     * Remove a control point from the river
     */
    public void removePoint(RiverPoint point) {
        points.remove(point);
        point.setParentRiver(null);
        updatePointIndices();
        recalculatePath();
        RiverRender.clearCacheForRiver(this);
    }

    /**
     * Update order indices after modification
     */
    private void updatePointIndices() {
        for (int i = 0; i < points.size(); i++) {
            points.get(i).setOrderIndex(i);
        }
    }

    /**
     * Get point at coordinates
     */
    public RiverPoint getPointAt(int x, int y) {
        for (RiverPoint point : points) {
            if (point.getX() == x && point.getY() == y) {
                return point;
            }
        }
        return null;
    }

    /**
     * Check if coordinates are on the calculated river path
     */
    public boolean isOnPath(int x, int y) {
        for (PathPoint p : calculatedPath) {
            if (p.getX() == x && p.getY() == y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recalculate the interpolated path through all control points
     */
    public void recalculatePath() {
        calculatedPath.clear();

        if (points.size() < 2) {
            return;
        }

        // Build path through all control points using Bresenham
        for (int i = 0; i < points.size() - 1; i++) {
            RiverPoint from = points.get(i);
            RiverPoint to = points.get(i + 1);

            List<Point> segment = getLinePoints(from.getX(), from.getY(), to.getX(), to.getY());

            // Skip first point for subsequent segments to avoid duplicates
            int startIdx = (i == 0) ? 0 : 1;
            for (int j = startIdx; j < segment.size(); j++) {
                Point p = segment.get(j);
                calculatedPath.add(new PathPoint(p.x, p.y));
            }
        }

        RiverRender.clearCacheForRiver(this);
    }

    /**
     * Bresenham's line algorithm
     */
    private List<Point> getLinePoints(int x1, int y1, int x2, int y2) {
        List<Point> result = new ArrayList<>();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int currentX = x1;
        int currentY = y1;

        while (true) {
            result.add(new Point(currentX, currentY));
            if (currentX == x2 && currentY == y2) break;

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
        return result;
    }


    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }

    public boolean isValid() {
        return points.size() >= 2;
    }

    @Override
    public void draw(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        // Draw river path if valid
        if (calculatedPath.size() >= 2) {
            RiverRender.drawRiver(this, g2d, offsetX, offsetY, zoom);
        }

        // Draw selection highlight
        if (isSelected()) {
            RiverRender.drawRiverSelection(this, g2d, offsetX, offsetY, zoom);
        }

        // Draw control points only in debug mode
        if (getWorld() != null && getWorld().getWorldScreen() instanceof GameWorldScreen s) {
            if (s.debugMode) {
                for (RiverPoint point : points) {
                    point.draw(g2d, offsetX, offsetY, zoom);
                }
            }
        }
    }
}