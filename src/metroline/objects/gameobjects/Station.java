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
import java.util.Map;
import java.util.Random;


/**
 * Station game object
 * TODO Чистка кода. Обобщение, разделение, комментирование. Вынести рендер в отдельный класс
 */
public class Station extends GameObject {
    private boolean skipTypeUpdate = false;
    private long constructionDate; // дата постройки в миллисекундах
    private float wearLevel = 0f; // степень износа (0..1)
    private boolean wasRepaired = false; // была ли станция отремонтирована
  //  private float accumulatedRevenue = 0f;
    private Map<Direction, Station> connections = new EnumMap<>(Direction.class);
    private StationColors color;
    private StationType type;

    private Train currentTrain;

    private static final float WEAR_RATE_MULTIPLIER = 5.0f;

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
    public void setWearLevel(float wearLevel) {
        this.wearLevel = wearLevel;
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



    /**
     * Устанавливает поезд, который прибыл на станцию
     * @param train поезд, который остановился на станции
     */
    public void setCurrentTrain(Train train) {
        this.currentTrain = train;

    }

    /**
     * Получает поезд, который в данный момент на станции
     * @return поезд на станции или null, если станция свободна
     */
    public Train getCurrentTrain() {
        return currentTrain;
    }

    /**
     * Проверяет, есть ли поезд на станции
     * @return true если на станции есть поезд, false если станция свободна
     */
    public boolean hasTrain() {
        return currentTrain != null;
    }

    /**
     * Освобождает станцию (убирает ссылку на поезд)
     */
    public void clearTrain() {
        this.currentTrain = null;
    }

    /**
     * Проверяет, может ли поезд остановиться на этой станции
     * @return true если станция свободна и готова принять поезд
     */
    public boolean canAcceptTrain() {
        return currentTrain == null &&
                type != StationType.BUILDING &&
                type != StationType.PLANNED &&
                type != StationType.DESTROYED &&
                type != StationType.RUINED &&
                type != StationType.ABANDONED;
    }
    public void setName(String name) {
        this.name = name;
        // Обновляем метку, если она существует
        StationLabel stationLabel = getWorld().getLabelForStation(this);
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
        if (this.type == StationType.DEPO && newType != StationType.DEPO) {
            MetroLogger.logWarning("Cannot change DEPO station type");
            return;
        }
        if (newType == StationType.DEPO &&
                (this.type == StationType.PLANNED ||
                        this.type == StationType.BUILDING ||
                        this.type == StationType.DESTROYED)) {
            MetroLogger.logWarning("Cannot convert PLANNED/BUILDING/DESTROYED station to DEPO");
            return;
        }
        // Запрещаем недопустимые переходы между типами
        if (this.type == StationType.PLANNED && newType != StationType.BUILDING) {
    //        MetroLogger.logWarning("Attempt to change PLANNED station to " + newType +" - only BUILDING is allowed");
            return;
        }
        if (newType == StationType.CLOSED &&
                (this.type == StationType.PLANNED || this.type == StationType.BUILDING)) {
            return;
        }
        if (this.type == StationType.BUILDING && newType == StationType.PLANNED) {

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

                return;
            }
        }



        this.type = newType;


        // Обновляем метку, если она существует
        StationLabel stationLabel = getWorld().getLabelForStation(this);
        if (stationLabel != null) {
            stationLabel.setText(name);
        }

        // Уведомляем связанные туннели об изменении
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).updateConnectedTunnels(this);
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
     * Устанавливает флаг пропуска автоматического обновления типа
     */
    public void setSkipTypeUpdate(boolean skip) {
        this.skipTypeUpdate = skip;
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
                type == StationType.REPAIR ||
                type == StationType.CLOSED) {
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

    public void updateWear() {
        if (type == StationType.BUILDING || type == StationType.PLANNED) {
            return; // Строящиеся и планируемые станции не изнашиваются
        }

        long currentTime = getWorld().getGameTime().getCurrentTimeMillis();
        long age = currentTime - getConstructionDate();

        // Ускоренный износ для тестирования
        float effectiveAge = age * WEAR_RATE_MULTIPLIER;

        if (type == StationType.CLOSED) {
            // Закрытые станции изнашиваются быстрее
            wearLevel = Math.min(1f, effectiveAge / GameConstants.ABANDONED_THRESHOLD);

            if (effectiveAge >= GameConstants.ABANDONED_THRESHOLD) {
                setType(StationType.ABANDONED);
            }
        } else if (type != StationType.ABANDONED &&
                type != StationType.DESTROYED &&
                type != StationType.RUINED) {
            // Обычные станции
            wearLevel = Math.min(1f, effectiveAge / GameConstants.MAX_LIFETIME);

            if (effectiveAge >= GameConstants.MAX_LIFETIME) {
                // Станция становится разрушенной
                setType(StationType.RUINED);
            }
        }
    }

    public boolean canRepair() {
        // Станцию можно ремонтировать если:
        // 1. Она не новая (прошло достаточно времени с постройки/последнего ремонта)
        // 2. Уровень износа выше минимального
        // 3. Станция не в полностью разрушенном состоянии
        long age = getWorld().getGameTime().getCurrentTimeMillis() - getConstructionDate();
        long minAgeForRepair = (long)(GameConstants.REPAIR_THRESHOLD / WEAR_RATE_MULTIPLIER);

        return age >= minAgeForRepair &&
                wearLevel > 0.1f && // Минимальный износ для ремонта
                type != StationType.ABANDONED &&
                type != StationType.RUINED &&
                type != StationType.DESTROYED &&
                type != StationType.BUILDING &&
                type != StationType.PLANNED;
    }

    public void repair() {
        if (canRepair()) {
            wearLevel = Math.max(0f, wearLevel - 0.5f); // Уменьшаем износ на 50%
            setConstructionDate(getWorld().getGameTime().getCurrentTimeMillis());
            wasRepaired = true;

            // Если станция была закрыта, открываем ее после ремонта
            if (type == StationType.CLOSED) {
                updateType(); // Автоматически определит правильный тип
            }
        }
    }

    public float getRepairCost() {
        // Стоимость ремонта зависит от уровня износа и типа станции
        float baseCost = 1000f; // Базовая стоимость
        float wearMultiplier = wearLevel * 2f; // Чем больше износ, тем дороже ремонт
        float typeMultiplier = 1f;

        if (type == StationType.TRANSFER) {
            typeMultiplier = 1.5f;
        } else if (type == StationType.TERMINAL) {
            typeMultiplier = 1.2f;
        }

        return baseCost * wearMultiplier * typeMultiplier;
    }

    public float getWearLevel() {
        return wearLevel;
    }


}



