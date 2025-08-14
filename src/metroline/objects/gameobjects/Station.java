package metroline.objects.gameobjects;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.enums.Direction;
import metroline.objects.enums.StationColors;
import metroline.objects.enums.StationType;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;


/**
 * Station game object
 * TODO Чистка кода. Обобщение, разделение, комментирование. Вынести рендер в отдельный класс
 */
public class Station extends GameObject {



    private Map<Direction, Station> connections = new EnumMap<>(Direction.class);


    private StationColors color;
    private StationType type;


    public Station() {
        super(0, 0);
    }
    public Station(World world, int x, int y, StationColors color, StationType type) {
        super(x, y);
        this.setWorld(world);
        this.color = color;
        this.type = type;
        this.name = generateRandomName();
    }

    /**
     * Gets connected stations with their directions
     * @return Map of directions to connected stations
     */
    public Map<Direction, Station> getConnections() { return connections; }

    private String generateRandomName() {
        Random rand = new Random();
        return GameConstants.NAME_PARTS[rand.nextInt(GameConstants.NAME_PARTS.length)];
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        // Обновляем метку, если она существует
        Label label = getWorld().getLabelForStation(this);
        if (label != null) {
            label.setText(name);
        }
    }
/**
     * Gets the station color
     * @return Color of the station
     */
    public Color getColor() {
        return color.getColor();
    }
    /**
     * Gets the station color enum
     * @return StationColor enum value
     */
    public StationColors getStationColor() {
        return color;
    }
    /**
     * Sets the station color using StationColor enum
     * @param stationColor New station color
     */
    public void setStationColor(StationColors stationColor) {
        this.color = stationColor;
    }
    /**
     * Sets the station color
     * @param color New color
     */
    /**
     * Sets the station color using Color object
     * @param color New color
     */
    public void setColor(Color color) {
        this.color = StationColors.fromColor(color);
    }
    /**
     * Gets the station type
     * @return Station type
     */
    public StationType getType() { return type; }

    /**
     * Sets the station type
     * @param newType New station type
     */
    public void setType(StationType newType) {
        // Запрещаем недопустимые переходы между типами
        if (this.type == StationType.PLANNED && newType != StationType.BUILDING) {
    //        MetroLogger.logWarning("Attempt to change PLANNED station to " + newType +" - only BUILDING is allowed");
            return;
        }
        if (newType == StationType.CLOSED &&
                (this.type == StationType.PLANNED || this.type == StationType.BUILDING)) {
     //       MetroLogger.logWarning("Cannot close PLANNED or BUILDING station");
            return;
        }
        if (this.type == StationType.BUILDING && newType == StationType.PLANNED) {
     //       MetroLogger.logWarning("Attempt to change BUILDING station back to PLANNED");
            return;
        }

        // Для TRANSFER станций проверяем соседей
        if (newType == StationType.TRANSFER) {
            boolean hasDifferentColorNeighbor = false;
            for (Direction dir : Direction.getOrthogonalDirections()) {
                int nx = x + dir.getDx();
                int ny = y + dir.getDy();
                Station neighbor = getWorld().getStationAt(nx, ny);
                if (neighbor != null && !neighbor.getColor().equals(this.color)) {
                    hasDifferentColorNeighbor = true;
                    break;
                }
            }

            if (!hasDifferentColorNeighbor) {
            //    MetroLogger.logWarning("Attempt to set TRANSFER type without different color neighbors");
                return;
            }
        }

       // MetroLogger.logInfo("Changing station " + getName() + " type from " + this.type + " to " + newType);

        this.type = newType;

        // Обновляем метку, если она существует
        Label label = getWorld().getLabelForStation(this);
        if (label != null) {
            label.setText(name);
        }

        // Уведомляем связанные туннели об изменении
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).updateConnectedTunnels(this);
        }
    }

    public boolean connectStation(Station other) {
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
        if (this.type != StationType.PLANNED && this.type != StationType.BUILDING) {
            this.updateType();
        }
        if (other.type != StationType.PLANNED && other.type != StationType.BUILDING) {
            other.updateType();
        }

        return true;
    }
    public void clearConnections() {
        this.connections.clear();
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
        // Сохраняем особые типы
//        if (this.type == StationType.PLANNED || this.type == StationType.BUILDING) {
//            return;
//        }

        // Проверяем соседей
        boolean hasSameColorNeighbor = false;
        boolean hasDifferentColorNeighbor = false;

        for (Direction dir : Direction.values()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();
            Station neighbor = getWorld().getStationAt(nx, ny);
            if (neighbor != null) {
                if (neighbor.getColor().equals(this.color)) {
                    hasSameColorNeighbor = true;
                } else {
                    hasDifferentColorNeighbor = true;
                }
            }
        }
        StationType newType = StationType.REGULAR;
 

        if (hasDifferentColorNeighbor) {
            newType = StationType.TRANSFER;
        }

        else if (connections.size() == 0) {
            newType = StationType.REGULAR;
        } else if (connections.size() == 1 ) {
            newType = StationType.TERMINAL;
        } else if (connections.size() == 2 ) {
            newType = StationType.TRANSIT;
        }
        if(this.type != StationType.PLANNED) {

            if (this.type != newType) {

         //   MetroLogger.logInfo("Auto-updating station " + getName() + " type from " + this.type + " to " + newType);
            this.type = newType;
            }
        }
    }
    public boolean isOnWater() {
        WorldTile tile = getWorld().getWorldTile(getX(), getY());
        return tile != null && tile.isWater();
    }
@Override
public void draw(Graphics g, int offsetX, int offsetY, float zoom) {

    WorldTile tile = getWorld().getWorldTile(getX(), getY());
    System.out.println("isWater? " + tile.isWater());
    if (getWorld().isRoundStationsEnabled()) {

//            drawWorldColorRing(g, offsetX, offsetY, zoom);
//            drawRoundTransfer(g, offsetX, offsetY, zoom);
//            drawRoundStation(g, offsetX, offsetY, zoom);
//            if (selected) drawRoundSelection(g, offsetX, offsetY, zoom);

    } else {
//        drawWorldColorSquare(g, offsetX, offsetY, zoom);
//        drawRoundTransfer(g, offsetX, offsetY, zoom);
//        drawSquareStation(g, offsetX, offsetY, zoom);
//        if(selected) drawSquareSelection(g, offsetX, offsetY, zoom);
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



