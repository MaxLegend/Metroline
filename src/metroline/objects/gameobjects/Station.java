package metroline.objects.gameobjects;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.enums.Direction;
import metroline.objects.enums.StationColors;
import metroline.objects.enums.StationType;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;


/**
 * Station game object
 * TODO Чистка кода. Обобщение, разделение, комментирование. Вынести рендер в отдельный класс
 */
public class Station extends GameObject {

    private long constructionDate; // дата постройки в миллисекундах

    private float wearLevel = 0f; // степень износа (0..1)
    private boolean wasRepaired = false; // была ли станция отремонтирована

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
        this.constructionDate = world.getGameTime().getCurrentTimeMillis();
    }

    public boolean isConstructionDateVisible() {
       // setConstructionDate(getWorld().getGameTime().getCurrentTimeMillis());
        return getType() != StationType.PLANNED;
    }



    public boolean isLowIncomeStations() {
         return this.getType() == StationType.DROWNED ||
                 this.getType() == StationType.ABANDONED ||
                 this.getType() == StationType.BURNED ||
                 this.getType() == StationType.RUINED ||
                 this.getType() == StationType.BUILDING ||
                 this.getType() == StationType.CLOSED ||
                 this.getType() == StationType.DESTROYED ||
                 this.getType() == StationType.PLANNED;
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
    public void setConstructionDate(long constructionDate) {
        this.constructionDate = constructionDate;
    }
    public long getConstructionDate() {
        return constructionDate;
    }
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

    public void updateWear() {
        long currentTime = getWorld().getGameTime().getCurrentTimeMillis();

        long age = currentTime - getConstructionDate();

        if (type == StationType.CLOSED) {
            // Закрытые станции изнашиваются быстрее
            wearLevel = Math.min(1f, (float)age / GameConstants.ABANDONED_THRESHOLD);

            if (age >= GameConstants.ABANDONED_THRESHOLD) {
                setType(StationType.ABANDONED);
            }
        } else if (type != StationType.ABANDONED &&
                type != StationType.DESTROYED &&
                type != StationType.BUILDING &&
                type != StationType.PLANNED) {
            // Обычные станции
            wearLevel = Math.min(1f, (float)age / GameConstants.MAX_LIFETIME);

            if (age >= GameConstants.MAX_LIFETIME) {
                // Станция становится разрушенной
                setType(StationType.RUINED);
            }
        }
    }

    public boolean canRepair() {
        long age = getWorld().getGameTime().getCurrentTimeMillis() - getConstructionDate();
        return age >= GameConstants.REPAIR_THRESHOLD && wearLevel < 1f &&
                type != StationType.ABANDONED &&
                type != StationType.RUINED &&
                type != StationType.DESTROYED;
    }

    public void repair() {
        if (canRepair()) {
            wearLevel = 0.2f; // После ремонта износ снижается, но не до нуля
            setConstructionDate(getWorld().getGameTime().getCurrentTimeMillis());
            wasRepaired = true;
        }
    }

    public float getWearLevel() {
        return wearLevel;
    }

    public float calculateUpkeepCost() {
        if (isLowIncomeStations()) {
            return 0f; // В этом режиме станции ничего не стоят
        }

        WorldTile tile = getWorld().getWorldTile(getX(), getY());
        if (tile == null) {
            return 0f; // Если тайл не найден, станция бесплатна
        }

        float baseCost = GameConstants.BASE_STATION_UPKEEP;
        float perm = tile.getPerm();

        // Базовая формула: baseCost * (1 + perm) — perm теперь сильнее влияет
        float cost = 1 / (baseCost * perm); // Усиливаем влияние perm

        // 1. Износ станции (чем больше износ, тем дороже обслуживание)
        cost *= 1 + wearLevel * 2.5f; // Раньше было 0.3f, теперь 0.5f

        // 2. Тип станции (пересадочные дороже в обслуживании)
        if (type == StationType.TRANSFER) {
            cost *= 1.35f; // Раньше 1.2f, теперь 1.35f
        }

        // 3. Вода (если станция на воде или рядом — дороже)
        if (tile.isWater() || hasWaterNeighbor()) {
            cost *= 1.25f; // Раньше 1.15f, теперь 1.25f
        }

        // Округляем до десятых для читаемости
        return cost;
    }
    private boolean hasWaterNeighbor() {
        for (Direction dir : Direction.values()) {
            int nx = getX() + dir.getDx();
            int ny = getY() + dir.getDy();

            if (nx >= 0 && nx < getWorld().getWidth() &&
                    ny >= 0 && ny < getWorld().getHeight()) {
                WorldTile neighbor = getWorld().getWorldTile(nx, ny);
                if (neighbor != null && neighbor.isWater()) {
                    return true;
                }
            }
        }
        return false;
    }
}



