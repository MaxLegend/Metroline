package game.objects;

import game.core.GameObject;
import game.objects.enums.Direction;
import game.objects.enums.StationType;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Station game object
 */
public class Station extends GameObject {
    public static final Color[] COLORS = {
            new Color(150, 0, 0),    // Dark red
            new Color(0, 100, 0),    // Dark green
            new Color(0, 0, 150),    // Dark blue
            new Color(200, 100, 0)   // Dark orange
    };

    private Color color;
    private StationType type;
    private Map<Direction, Station> connections = new EnumMap<>(Direction.class);

    public Station(int x, int y, Color color, StationType type) {
        super(x, y);
        this.color = color;
        this.type = type;
    }

    /**
     * Gets the station color
     * @return Color of the station
     */
    public Color getColor() { return color; }

    /**
     * Sets the station color
     * @param color New color
     */
    public void setColor(Color color) { this.color = color; }

    /**
     * Gets the station type
     * @return Station type
     */
    public StationType getType() { return type; }

    /**
     * Sets the station type
     * @param type New station type
     */
    public void setType(StationType type) { this.type = type; }

    /**
     * Gets connected stations with their directions
     * @return Map of directions to connected stations
     */
    public Map<Direction, Station> getConnections() { return connections; }

    /**
     * Connects this station to another
     * @param other Station to connect to
     * @return True if connection was successful
     */
    public boolean connect(Station other) {
        Direction dir = getDirectionTo(other);
        Direction oppositeDir = dir.getOpposite();

        // Check if we can make this connection
        if (connections.size() >= 2) return false;
        if (connections.containsKey(oppositeDir)) return false;

        // Check if other station can accept this connection
        if (other.connections.size() >= 2) return false;
        if (other.connections.containsKey(dir)) return false;

        // Make the connection
        connections.put(oppositeDir, other);
        other.connections.put(dir, this);
        return true;
    }

    /**
     * Disconnects this station from another
     * @param other Station to disconnect from
     */
    public void disconnect(Station other) {
        Direction dirToRemove = null;
        for (Map.Entry<Direction, Station> entry : connections.entrySet()) {
            if (entry.getValue() == other) {
                dirToRemove = entry.getKey();
                break;
            }
        }

        if (dirToRemove != null) {
            connections.remove(dirToRemove);
            other.connections.remove(dirToRemove.getOpposite());
        }
    }

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(16 * zoom); // Half size of tile
        int drawX = (int)((getX() * 32 + offsetX + 8) * zoom); // Centered
        int drawY = (int)((getY() * 32 + offsetY + 8) * zoom); // Centered

        // Draw station based on type
        g.setColor(color);
        switch (type) {
            case REGULAR:
                g.fillRect(drawX, drawY, drawSize, drawSize);
                break;
            case TRANSFER:
                g.fillOval(drawX, drawY, drawSize, drawSize);
                break;
            case TERMINAL:
                int[] xPoints = {drawX, drawX + drawSize/2, drawX + drawSize, drawX + drawSize/2};
                int[] yPoints = {drawY + drawSize/2, drawY, drawY + drawSize/2, drawY + drawSize};
                g.fillPolygon(xPoints, yPoints, 4);
                break;
            case TRANSIT:
                g.fillRect(drawX, drawY + drawSize/4, drawSize, drawSize/2);
                g.fillRect(drawX + drawSize/4, drawY, drawSize/2, drawSize);
                break;
        }

        // Draw selection indicator
        if (selected) {
            g.setColor(Color.YELLOW);
            g.drawOval(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4);
        }
    }

    /**
     * Gets the direction from this station to another
     * @param other Target station
     * @return Direction to the other station
     */
    private Direction getDirectionTo(Station other) {
        int dx = other.getX() - x;
        int dy = other.getY() - y;

        if (dx == 0 && dy < 0) return Direction.NORTH;
        if (dx > 0 && dy < 0) return Direction.NORTHEAST;
        if (dx > 0 && dy == 0) return Direction.EAST;
        if (dx > 0 && dy > 0) return Direction.SOUTHEAST;
        if (dx == 0 && dy > 0) return Direction.SOUTH;
        if (dx < 0 && dy > 0) return Direction.SOUTHWEST;
        if (dx < 0 && dy == 0) return Direction.WEST;
        if (dx < 0 && dy < 0) return Direction.NORTHWEST;

        return Direction.NORTH; // default
    }

    /**
     * Выводит отладочную информацию о станции
     */
    public void printDebugInfo() {
        System.out.println("=== Station Debug Info ===");
        System.out.println("Position: (" + x + "," + y + ")");
        System.out.println("Type: " + type);
        System.out.println("Color: " + color);
        System.out.println("Connections: " + connections.size());

        for (Map.Entry<Direction, Station> entry : connections.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> Station@" + entry.getValue().hashCode());
        }

        System.out.println("HashCode: " + hashCode());
    }
}



