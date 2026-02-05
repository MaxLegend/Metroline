package metroline.objects.gameobjects;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.core.world.tiles.WorldTile;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.Direction;
import metroline.objects.enums.StationColors;
import metroline.objects.enums.StationType;
import metroline.util.MetroLogger;

import java.awt.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;


/**
 * Station game object
 *
 */
public class Station extends GameObject {

  //  private float accumulatedRevenue = 0f;
    private Map<Direction, Station> connections = new EnumMap<>(Direction.class);
    private StationColors color;
    private StationType type;

    private StationLabel label;

    private Map<String, String> customProperties = new HashMap<>();
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
     * Gets the station label
     * @return StationLabel associated with this station
     */
    public StationLabel getLabel() {
        return label;
    }

    /**
     * Sets the station label
     * @param label StationLabel to associate with this station
     */
    public void setLabel(StationLabel label) {
        this.label = label;
    }
    /**
     * Проверяет, есть ли соседняя станция того же цвета в радиусе одной клетки
     * @return true если есть соседняя станция того же цвета
     */
    public boolean hasSameColorNeighbor() {
        for (Direction dir : Direction.getOrthogonalDirections()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();
            Station neighbor = getWorld().getStationAt(nx, ny);

            if (neighbor != null &&
                    neighbor.getStationColor() == this.color &&
                    neighbor.getType() != StationType.PLANNED &&
                    neighbor.getType() != StationType.BUILDING &&
                    neighbor.getType() != StationType.DESTROYED) {
                return true;
            }
        }
        return false;
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
        StationLabel stationLabel = this.getLabel();
        if (stationLabel != null) {
            stationLabel.setText(name);
        }
    }
    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }
    @Override
    public void draw(Graphics2D g, int offsetX, int offsetY, float zoom) {
    }
    /**
     * Auto-determine type when REGULAR is set manually
     * Used only when user explicitly sets REGULAR type
     */
    public void autoDetectTypeFromRegular() {
        // Check connections first
        if (connections.size() == 1) {
            this.type = StationType.TERMINAL;
        } else if (connections.size() == 2) {
            this.type = StationType.TRANSIT;
        }

        // Check for transfer (different color neighbor) - overrides connection check
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

        if (hasDifferentColorNeighbor) {
            this.type = StationType.TRANSFER;
        }

        // Update label if exists
        StationLabel stationLabel = getLabel();
        if (stationLabel != null) {
            stationLabel.setText(name);
        }

        // Notify connected tunnels
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).updateConnectedTunnels(this);
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
        // Allow direct type change without restrictions for manual editing
        this.type = newType;

        // Update label if exists
        StationLabel stationLabel = getLabel();
        if (stationLabel != null) {
            stationLabel.setText(name);
        }

        // Trigger repaint if world exists
        if (getWorld() != null) {
            // Log type change
            MetroLogger.logInfo("[Station::setType] Changed station type to: " + newType);
        }
    }

    public boolean isTerminal() {
        return type == StationType.TERMINAL || connections.size() == 1;
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

        if (type == StationType.DESTROYED ||
                type == StationType.BURNED ||
                type == StationType.DEPO ||
                type == StationType.DROWNED ||
                type == StationType.RUINED ||
                type == StationType.ABANDONED ||
                type == StationType.BUILDING ||
                type == StationType.CLOSED ||
                type == StationType.REPAIR) {
            return;
        }
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
        StationType newType;
        if(this.type == StationType.DEPO) {
            newType = StationType.DEPO;
        } else {
            newType = StationType.REGULAR;
        }



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

            this.type = newType;
            }
        }
    }
    public boolean isOnWater() {
        WorldTile tile = getWorld().getWorldTile(getX(), getY());
        return tile != null && tile.isWater();
    }





    /**
     * Gets the direction from this station to another
     * @param other Target station
     * @return Direction to the other station
     */
    Direction getDirectionTo(Station other) {
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

    public Map<String, String> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, String> properties) {
        this.customProperties = (properties != null) ? properties : new HashMap<>();
    }
    /**
     * Проверяет, находится ли станция визуально "внутри" линии реки.
     * Проверка строгая: станция не должна касаться только краешком.
     */
    public boolean isOnRiver() {
        if (!(getWorld() instanceof GameWorld)) return false;
        GameWorld gw = (GameWorld) getWorld();

        // Координаты центра станции в пикселях
        double sX = this.x * 32 + 16;
        double sY = this.y * 32 + 16;

        // Радиус станции (визуальный, примерно половина ширины в 24px)
        double stationRadius = 8.0;

        // Ширина реки (берем стандартную 20f, или можно вынести в константу)
        double riverWidth = 20.0;
        double riverHalfWidth = riverWidth / 2.0;

        // Перебираем все реки в мире
        // (Предполагаем, что у GameWorld есть метод getRivers() или getGameObjects())
        // Если getRivers() нет, используйте world.getGameObjects().stream().filter(o -> o instanceof River)...
        for (River river : gw.getRivers()) {
            List<RiverPoint> points = river.getPoints();
            if (points.size() < 2) continue;

            // Проверяем каждый сегмент реки
            for (int i = 0; i < points.size() - 1; i++) {
                RiverPoint p1 = points.get(i);
                RiverPoint p2 = points.get(i + 1);

                double p1x = p1.getX() * 32 + 16;
                double p1y = p1.getY() * 32 + 16;
                double p2x = p2.getX() * 32 + 16;
                double p2y = p2.getY() * 32 + 16;

                double dist = pointToSegmentDistance(sX, sY, p1x, p1y, p2x, p2y);

                // УСЛОВИЕ: Станция "полностью" внутри.
                // Расстояние от центра + радиус станции должны быть меньше половины ширины реки.
                // dist + stationRadius <= riverHalfWidth
                // Для мягкости можно дать небольшой допуск (например 1-2 пикселя),
                // так как станция (24px) шире реки (20px), строгое условие никогда не сработает.
                // Поэтому проверяем, лежит ли ЦЕНТР станции близко к центру реки.

                if (dist <= 4.0) { // Если центр станции отклоняется от центра реки не более чем на 4 пикселя
                    return true;
                }
            }
        }
        return false;
    }

    // Математика: расстояние от точки (px,py) до отрезка (x1,y1)-(x2,y2)
    private double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (l2 == 0) return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));

        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));

        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);

        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }
    public void setProperty(String key, String value) {
        if (key != null && !key.trim().isEmpty()) {
            key = key.trim();
            if (value == null || value.trim().isEmpty()) {
                customProperties.remove(key);
            } else {
                customProperties.put(key, value.trim());
            }
        }
    }

    public String getProperty(String key) {
        return customProperties.get(key);
    }

}



