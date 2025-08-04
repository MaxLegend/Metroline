package game.objects;

import game.core.GameObject;
import game.objects.enums.Direction;
import game.objects.enums.StationType;
import screens.WorldGameScreen;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;


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

    private String name;

    private static final String[] NAME_PARTS = {"Pyatorocka", "Magnit", "Nizhni", "Kotova", "Baton", "Butilka",
            "Lolkek", "Tesmio", "Geroyev", "Svetofor", "Ploshad", "Proezd",
            "Sobakov", "Balka", "Ulitca", "Bred"};

    private Color color;
    private StationType type;
    private Map<Direction, Station> connections = new EnumMap<>(Direction.class);

    public Station(int x, int y, Color color, StationType type) {
        super(x, y);
        this.color = color;
        this.type = type;
        this.name = generateRandomName();
    }
    private String generateRandomName() {
        Random rand = new Random();
        return NAME_PARTS[rand.nextInt(NAME_PARTS.length)];
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        // Обновляем метку, если она существует
        Label label = WorldGameScreen.getInstance().world.getLabelForStation(this);
        if (label != null) {
            label.setText(name);
        }
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
    public boolean connect(Station other) {
        Direction dir = getDirectionTo(other);
        Direction oppositeDir = dir.getOpposite();

        // Check if we can make this connection
        if (connections.size() >= 2) return false;
        if (connections.containsKey(oppositeDir)) return false;

        // Check if other station can accept this connection
        if (other.connections.size() >= 2) return false;
        if (other.connections.containsKey(dir)) return false;
        if(other.getColor() != this.getColor()) return false;
        // Make the connection
        connections.put(oppositeDir, other);
        other.connections.put(dir, this);

        // Update types for both stations
        this.updateType();
        other.updateType();

        return true;
    }

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

            // Update types for both stations
            this.updateType();
            other.updateType();
        }
    }

    /**
     * Automatically determines and updates station type based on connections
     */
    public void updateType() {
        int connectionCount = connections.size();

        // Check for TRANSFER condition (adjacent station of different color)
        boolean hasDifferentColorNeighbor = false;
        for (Station neighbor : connections.values()) {
            if (!neighbor.getColor().equals(this.color)) {
                hasDifferentColorNeighbor = true;
                break;
            }
        }

        // Determine new type
        if (connectionCount == 0) {
            type = StationType.REGULAR;
        } else if (connectionCount == 1) {
            type = hasDifferentColorNeighbor ? StationType.TRANSFER : StationType.TERMINAL;
        } else if (connectionCount == 2) {
            type = StationType.TRANSIT;
        }
    }

@Override
public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
    int drawSize = (int)(24 * zoom);
    int drawX = (int)((getX() * 32 + offsetX + 4) * zoom);
    int drawY = (int)((getY() * 32 + offsetY + 4) * zoom);
    int arcSize = (int)(drawSize * 0.35);

    Graphics2D g2d = (Graphics2D)g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Draw station based on type
    g2d.setColor(color);
    switch (type) {
        case REGULAR:
        case TERMINAL:
        case TRANSIT:
            g2d.fillRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
            break;


        case TRANSFER:
            // Rounded square with connection indicator
            g2d.fillRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            // Find adjacent station of different color
            for (Station neighbor : connections.values()) {
                if (!neighbor.getColor().equals(this.color)) {
                    // Draw connection indicator
                    g2d.setColor(neighbor.getColor());
                    int neighborX = (int)((neighbor.getX() * 32 + offsetX + 16) * zoom);
                    int neighborY = (int)((neighbor.getY() * 32 + offsetY + 16) * zoom);

                    // Calculate direction and draw connector
                    Direction dir = getDirectionTo(neighbor);
                    drawTransferConnector(g2d, drawX, drawY, drawSize, neighborX, neighborY, dir);
                    break;
                }
            }
            break;

    }
    //решить проблему почему соединяются станции разных цветов
    // Draw selection indicator
    if (selected) {
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.drawRoundRect(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4, arcSize, arcSize);
    }

}



    private void drawTransferConnector(Graphics2D g, int x1, int y1, int size, int x2, int y2, Direction dir) {
        int connectorSize = size / 2;
        int centerX = x1 + size/2;
        int centerY = y1 + size/2;

        switch (dir) {
            case NORTH:
                g.fillRoundRect(centerX - connectorSize/2, y1 - connectorSize,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
            case SOUTH:
                g.fillRoundRect(centerX - connectorSize/2, y1 + size,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
            case EAST:
                g.fillRoundRect(x1 + size, centerY - connectorSize/2,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
            case WEST:
                g.fillRoundRect(x1 - connectorSize, centerY - connectorSize/2,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
            // Add other directions as needed
            default:
                // Draw line for diagonal directions
                g.setStroke(new BasicStroke(size/4));
                g.drawLine(centerX, centerY, x2, y2);
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

}



