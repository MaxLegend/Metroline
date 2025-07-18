package game.objects;

import game.core.GameObject;
import game.tiles.GameTile;

import java.awt.*;
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

        // Determine if we need a bend
        boolean sameRow = y1 == y2;
        boolean sameCol = x1 == x2;
        boolean diagonal = Math.abs(x1 - x2) == Math.abs(y1 - y2);
        GameTile gameTile;
        if (sameRow || sameCol || diagonal) {
            // Straight line
            addStraightPath(x1, y1, x2, y2);
        } else {
            // One bend path
            if (pathPoint == null) {
                // Create default control PathPoint (L-shaped path)

                pathPoint = new PathPoint(x1, y2);
                if(pathPoint.getTile(x1, x2) instanceof GameTile) {
                    gameTile = (GameTile) pathPoint.getTile(x1, x2);

                }
            }
            addBendPath(x1, y1, pathPoint.getX(), pathPoint.getY(), x2, y2);

        }
    }

    private void addStraightPath(int x1, int y1, int x2, int y2) {

        if (x1 != x2 || y1 != y2) {
            PathPoint newPathPoint = new PathPoint(getX(), getY());
            path.add(newPathPoint);
        }
        path.add(new PathPoint(x2, y2));
    }
    private void addBendPath(int x1, int y1, int cx, int cy, int x2, int y2) {
        // First segment to control PathPoint
        addStraightPath(x1, y1, cx, cy);

        // Second segment from control PathPoint to end
        // Skip the control PathPoint to avoid duplicate
        path.remove(path.size() - 1);
        addStraightPath(cx, cy, x2, y2);
    }
    /**
     * Moves the control PathPoint for this tunnel
     * @param x New x coordinate
     * @param y New y coordinate
     */
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
        g2d.setStroke(new BasicStroke(3 * zoom));

        PathPoint prev = path.get(0);
        for (int i = 1; i < path.size(); i++) {
            PathPoint current = path.get(i);

            int x1 = (int)((prev.getX() * 32 + offsetX + 16) * zoom);
            int y1 = (int)((prev.getY() * 32 + offsetY + 16) * zoom);
            int x2 = (int)((current.getX() * 32 + offsetX + 16) * zoom);
            int y2 = (int)((current.getY() * 32 + offsetY + 16) * zoom);

            g2d.drawLine(x1, y1, x2, y2);
            prev = current;
        }

        // Draw control PathPoints if selected or in edit mode
        if (selected || pathPoint != null) {
            g2d.setColor(Color.BLACK);
            for (PathPoint p : path) {
                int x = (int)((p.getX() * 32 + offsetX + 16) * zoom);
                int y = (int)((p.getY() * 32 + offsetY + 16) * zoom);
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
        System.out.println("Path PathPoints: " + path.size());

        for (int i = 0; i < path.size(); i++) {
            PathPoint p = path.get(i);
            System.out.println("  " + i + ": (" + p.x + "," + p.y + ")");
        }

        if (pathPoint != null) {
            System.out.println("Control PathPoint: (" + pathPoint.x + "," + pathPoint.y + ")");
        }

        System.out.println("HashCode: " + hashCode());
    }
}

