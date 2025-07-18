package game.objects;

import game.GameObject;
import game.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tunnel game object connecting stations
 */
public class Tunnel extends GameObject {
    private Station start;
    private Station end;
    private List<Point> path = new ArrayList<>();
    private Point controlPoint; // For single bend tunnels


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
     * Gets the path points
     * @return List of path points
     */
    public List<Point> getPath() { return path; }

    /**
     * Recalculates the path between stations with maximum one bend
     */
    public void calculatePath() {
        path.clear();

        int x1 = start.getX();
        int y1 = start.getY();
        int x2 = end.getX();
        int y2 = end.getY();

        // Determine if we need a bend
        boolean sameRow = y1 == y2;
        boolean sameCol = x1 == x2;
        boolean diagonal = Math.abs(x1 - x2) == Math.abs(y1 - y2);

        if (sameRow || sameCol || diagonal) {
            // Straight line
            addStraightPath(x1, y1, x2, y2);
        } else {
            // One bend path
            if (controlPoint == null) {
                // Create default control point (L-shaped path)
                controlPoint = new Point(x1, y2);
            }
            addBendPath(x1, y1, controlPoint.x, controlPoint.y, x2, y2);
        }
    }

    private void addStraightPath(int x1, int y1, int x2, int y2) {
        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);

        int x = x1;
        int y = y1;

        while (x != x2 || y != y2) {
            path.add(new Point(x, y));
            x += dx;
            y += dy;
        }
        path.add(new Point(x2, y2));
    }
    private void addBendPath(int x1, int y1, int cx, int cy, int x2, int y2) {
        // First segment to control point
        addStraightPath(x1, y1, cx, cy);

        // Second segment from control point to end
        // Skip the control point to avoid duplicate
        path.remove(path.size() - 1);
        addStraightPath(cx, cy, x2, y2);
    }
    /**
     * Moves the control point for this tunnel
     * @param x New x coordinate
     * @param y New y coordinate
     */
    public void moveControlPoint(int x, int y) {
        this.controlPoint = new Point(x, y);
        System.out.println("Moving control point to: " + x + "," + y);
        calculatePath();
    }
    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        if (path.size() < 2) return;

        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(start.getColor());
        g2d.setStroke(new BasicStroke(3 * zoom));

        Point prev = path.get(0);
        for (int i = 1; i < path.size(); i++) {
            Point current = path.get(i);

            int x1 = (int)((prev.x * 32 + offsetX + 16) * zoom);
            int y1 = (int)((prev.y * 32 + offsetY + 16) * zoom);
            int x2 = (int)((current.x * 32 + offsetX + 16) * zoom);
            int y2 = (int)((current.y * 32 + offsetY + 16) * zoom);

            g2d.drawLine(x1, y1, x2, y2);
            prev = current;
        }

        // Draw control points if selected or in edit mode
        if (selected || controlPoint != null) {
            g2d.setColor(Color.YELLOW);
            for (Point p : path) {
                int x = (int)((p.x * 32 + offsetX + 16) * zoom);
                int y = (int)((p.y * 32 + offsetY + 16) * zoom);
                g2d.fillOval(x - 3, y - 3, 6, 6);
            }
        }
    }

    /**
     * Выводит отладочную информацию о туннеле
     */
    public void printDebugInfo() {
        System.out.println("=== Tunnel Debug Info ===");
        System.out.println("Start: (" + start.getX() + "," + start.getY() + ")");
        System.out.println("End: (" + end.getX() + "," + end.getY() + ")");
        System.out.println("Path points: " + path.size());

        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            System.out.println("  " + i + ": (" + p.x + "," + p.y + ")");
        }

        if (controlPoint != null) {
            System.out.println("Control point: (" + controlPoint.x + "," + controlPoint.y + ")");
        }

        System.out.println("HashCode: " + hashCode());
    }
}

