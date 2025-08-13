package metroline.core.world;

import metroline.core.time.GameTime;
import metroline.core.world.tiles.GameTile;

import metroline.core.world.tiles.WorldTile;
import metroline.objects.enums.Direction;
import metroline.objects.enums.GameplayUnitsType;
import metroline.objects.gameobjects.*;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.MainFrame;
import metroline.objects.gameobjects.Label;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.util.LngUtil;
import metroline.util.MessageUtil;
import metroline.util.MetroLogger;
import metroline.util.MetroSerializer;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * TODO ПОПРАВИТЬ ВОЗМОЖНОСТЬ РАСПОЛОЖЕНИЯ МЕТОК
 */

public class GameWorld extends World {
    private transient MainFrame mainFrame;
    private static String SAVE_FILE = "game_save.metro";
    public float money;

    private long stationDestroyTime = 100000; // Время разрушения станции
    private long tunnelDestroyTime = 100000;
    private long stationBuildTime = 100000;
    private long tunnelBuildTime = 100000;

    private List<GameplayUnits> gameplayUnits = new ArrayList<>();

    public transient Map<Station, Long> stationDestructionStartTimes = new HashMap<>();
    public transient Map<Station, Long> stationDestructionDurations = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelDestructionStartTimes = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelDestructionDurations = new HashMap<>();

    public transient Map<Station, Long> stationBuildStartTimes = new HashMap<>();
    public transient Map<Station, Long> stationBuildDurations = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelBuildStartTimes = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelBuildDurations = new HashMap<>();


    public transient LinesLegendWindow legendWindow;
    public GameWorld() {
        super();
        initTransientFields();
    }

    public GameWorld(int width, int height,boolean hasPassengerCount, boolean hasAbilityPay,  boolean hasLandscape, boolean hasRivers, Color worldColor, int money) {
        super(null, width, height, hasPassengerCount, hasAbilityPay, hasLandscape,hasRivers,worldColor, SAVE_FILE);
        this.mainFrame = MainFrame.getInstance();
        this.money = money;
        initTransientFields();

    }

    public void initTransientFields() {
        if (stationDestructionStartTimes == null) {
            stationDestructionStartTimes = new HashMap<>();
        }
        if (stationDestructionDurations == null) {
            stationDestructionDurations = new HashMap<>();
        }
        if (tunnelDestructionStartTimes == null) {
            tunnelDestructionStartTimes = new HashMap<>();
        }
        if (tunnelDestructionDurations == null) {
            tunnelDestructionDurations = new HashMap<>();
        }
        if (stationBuildStartTimes == null) {
            stationBuildStartTimes = new HashMap<>();
        }
        if (stationBuildDurations == null) {
            stationBuildDurations = new HashMap<>();
        }
        if (tunnelBuildStartTimes == null) {
            tunnelBuildStartTimes = new HashMap<>();
        }
        if (tunnelBuildDurations == null) {
            tunnelBuildDurations = new HashMap<>();
        }
    }
    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
    }
    public void updateLegendWindow() {
        if (mainFrame != null && mainFrame.legendWindow != null) {
            mainFrame.legendWindow.updateLegend(this);
        }
    }
    public PathPoint findFreePositionNear(int x, int y, String name) {
        // Сортируем направления по приоритету (включая диагонали)
        List<Direction> priorityDirections = Arrays.asList(
                Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH,
                Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHEAST, Direction.NORTHWEST
        );

        for (Direction dir : priorityDirections) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();

            if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                    getStationAt(nx, ny) == null && getLabelAt(nx, ny) == null&& getGameplayUnitsAt(nx, ny) == null) {

                // Проверяем, что текст не будет перекрывать станцию
                if (isTextPositionGood(dir, name)) {
                    return new PathPoint(nx, ny);
                }
            }
        }

        return null;
    }
    @Override
    public void addStation(Station station) {
        super.addStation(station);

        if (station.getType() == StationType.BUILDING) {
            // Проверяем, не добавлена ли уже станция
            if (!stationBuildStartTimes.containsKey(station)) {
                long startTime = gameTime.getCurrentTimeMillis();
                stationBuildStartTimes.put(station, startTime);
                stationBuildDurations.put(station, stationBuildTime);

                if(startTime % 2000 == 0) MetroLogger.logInfo("Station construction REGISTERED: " + station.getName() +
                        " | Start: " + startTime +
                        " | Duration: " + stationBuildTime + "ms" +
                        " | Expected finish: " + (startTime + stationBuildTime));
            } else {
                MetroLogger.logWarning("Station already in construction: " + station.getName());
            }
        }


    }
    @Override
    public void addTunnel(Tunnel tunnel) {


        // Вызываем родительский метод
            super.addTunnel(tunnel);

        // Инициализируем карты, если они null
        if (tunnelBuildStartTimes == null) {

            tunnelBuildStartTimes = new HashMap<>();
        }
        if (tunnelBuildDurations == null) {

            tunnelBuildDurations = new HashMap<>();
        }

        if (tunnel.getType() == TunnelType.BUILDING) {

            long startTime = gameTime.getCurrentTimeMillis();
            tunnelBuildStartTimes.put(tunnel, startTime);
            tunnelBuildDurations.put(tunnel, tunnelBuildTime);

            // Устанавливаем стоимость строительства в зависимости от длины
            int lengthBasedCost = tunnel.getLength() * 10; // Например, 10 за сегмент
            tunnelBuildTime = Math.max(50000, lengthBasedCost * 1000); // Минимум 50 секунд

        }

    }
    public GameplayUnits getGameplayUnitsAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        GameObject obj = gameGrid[x][y].getContent();
        return obj instanceof GameplayUnits ? (GameplayUnits)obj : null;
    }
    public float calculateStationRevenue(Station station) {
        // Получаем тайл, на котором стоит станция
        WorldTile tile = getWorldTile(station.getX(), station.getY());

        // Базовый доход
        float revenue = GameConstants.STATION_BASE_REVENUE;

        // Модификаторы:
        // 1. Учитываем permission (чем больше perm, тем больше доход)
        //    Диапазон perm: 0-1, добавляем 1 чтобы избежать умножения на 0
        float permModifier = 1 + (tile.getPerm()); // От 1.0 до 2.0

        // 2. Учитываем платежеспособность (abilityPay)
        //    Диапазон abilityPay: 0-1.5 (судя по generatePaymentZones)
        float abilityPayModifier = 1 + (1/tile.getAbilityPay()); // От 1.0 до 2.5

        // 3. Учитываем пассажиропоток (passengerCount)
        //    Нормализуем значение (предполагаем макс 2000 пассажиров)
        float passengerModifier = 1 + (float) tile.getPassengerCount() / 2000; // От 1.0 до 2.0

        // Итоговый расчет
        revenue = revenue * permModifier * abilityPayModifier * passengerModifier;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = station.getX() + dx;
                int ny = station.getY() + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    GameObject obj = gameGrid[nx][ny].getContent();
                    if (obj instanceof GameplayUnits) {
                        revenue *= ((GameplayUnits) obj).getType().getIncomeMultiplier();
                    }
                }
            }
        }


        return revenue;
    }
    public void updateStationsRevenue() {
        for (Station station : stations) {
            // Пропускаем станции, которые строятся или разрушаются
            if (station.getType() == StationType.BUILDING ||
                    station.getType() == StationType.CLOSED ||
                    station.getType() == StationType.DESTROYED ||
                    station.getType() == StationType.PLANNED) {
                continue;
            }

            float revenue = calculateStationRevenue(station);
            addMoney(revenue);
        }
    }
    private float calculateProgress(long startTime, long duration) {
        long currentTime = gameTime.getCurrentTimeMillis();
        if (startTime > currentTime) {
            return 0f;
        }
        return Math.min(1.0f, (float)(currentTime - startTime) / duration);
    }
    public float getStationConstructionProgress(Station station) {
        if (!stationBuildStartTimes.containsKey(station)) {
            return 0f; // Возвращаем 0 вместо 1, чтобы было заметно
        }

        if (station.getType() == StationType.BUILDING && stationBuildStartTimes.containsKey(station)) {

            return calculateProgress(stationBuildStartTimes.get(station), stationBuildDurations.get(station));
        } else if (station.getType() == StationType.DESTROYED && stationDestructionStartTimes.containsKey(station)) {

            return 1.0f - calculateProgress(stationDestructionStartTimes.get(station), stationDestructionDurations.get(station));
        }
        return 0f;
    }


    public float getTunnelConstructionProgress(Tunnel tunnel) {
        if (!tunnelBuildStartTimes.containsKey(tunnel)) {
            return 1.0f;
        }

        if (tunnel.getType() == TunnelType.BUILDING && tunnelBuildStartTimes.containsKey(tunnel)) {
            return calculateProgress(tunnelBuildStartTimes.get(tunnel),
                    tunnelBuildDurations.get(tunnel));
        } else if (tunnel.getType() == TunnelType.DESTROYED && tunnelDestructionStartTimes.containsKey(tunnel)) {
            return 1.0f - calculateProgress(tunnelDestructionStartTimes.get(tunnel),
                    tunnelDestructionDurations.get(tunnel));
        }
        return 1.0f;
    }
    public void startDestroyingTunnel(Tunnel tunnel) {
        if (tunnel.getType() != TunnelType.ACTIVE) {
            MetroLogger.logWarning("Cannot destroy tunnel of type " + tunnel.getType());
            return;
        }

        tunnel.setType(TunnelType.DESTROYED);
        long startTime = gameTime.getCurrentTimeMillis();
        tunnelDestructionStartTimes.put(tunnel, startTime);
        tunnelDestructionDurations.put(tunnel, tunnelDestroyTime);

        MetroLogger.logInfo("Tunnel destruction started");
    }
    public void startDestroyingStation(Station station) {
        if (station.getType() != StationType.REGULAR &&
                station.getType() != StationType.TRANSFER &&
                station.getType() != StationType.TERMINAL &&
                station.getType() != StationType.CLOSED &&
                station.getType() != StationType.TRANSIT) {
            MetroLogger.logWarning("Cannot destroy station of type " + station.getType());
         //   return;
        }

        station.setType(StationType.DESTROYED);
        long startTime = gameTime.getCurrentTimeMillis();
        stationDestructionStartTimes.put(station, startTime);
        stationDestructionDurations.put(station, stationDestroyTime);

        MetroLogger.logInfo("Station destruction started: " + station.getName());
    }

    public List<GameplayUnits> getGameplayUnits() {
        return gameplayUnits;
    }
    /**
     * Gets the any game object at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return GameObject or null if none exists
     */
    @Override
    public GameObject getGameObjectAt(int x, int y) {
        // Сначала проверяем станции
        Station station = this.getStationAt(x, y);
        if (station != null) {
            return station;
        }

        // Затем проверяем туннели
        Tunnel tunnel = this.getTunnelAt(x, y);
        if (tunnel != null) {
            return tunnel;
        }

        // Затем проверяем метки (если нужно)
        Label label = this.getLabelAt(x, y);
        if (label != null) {
            return label;
        }
        GameplayUnits gUnits = this.getGameplayUnitsAt(x, y);
        if (gUnits != null) {
            return gUnits;
        }
        // Если ничего не найдено
        return null;
    }
    public void addGameplayUnits(GameplayUnits obj) {
        gameplayUnits.add(obj);
        gameGrid[obj.getX()][obj.getY()].setContent(obj);
        WorldGameScreen.getInstance().invalidateCache();
    }

    public void removeGameplayUnits(GameplayUnits obj) {
        gameplayUnits.remove(obj);
        gameGrid[obj.getX()][obj.getY()].setContent(null);
        WorldGameScreen.getInstance().invalidateCache();
    }
    public GameTime getGameTime() {
        return gameTime;
    }
    public void generateRandomGameplayUnits(int count) {
        Random random = new Random();
        GameplayUnitsType[] types = GameplayUnitsType.values();

        // Разбиваем карту на секции для равномерного распределения
        int sectionsX = (int) Math.sqrt(count) + 1;
        int sectionsY = (int) Math.sqrt(count) + 1;
        int sectionWidth = width / sectionsX;
        int sectionHeight = height / sectionsY;

        int objectsPerSection = count / (sectionsX * sectionsY) + 1;
        int generatedCount = 0;

        for (int sx = 0; sx < sectionsX && generatedCount < count; sx++) {
            for (int sy = 0; sy < sectionsY && generatedCount < count; sy++) {
                // Генерируем объекты в текущей секции
                for (int i = 0; i < objectsPerSection && generatedCount < count; i++) {
                    int attempts = 0;
                    boolean placed = false;

                    // Делаем несколько попыток разместить объект в секции
                    while (!placed && attempts < 10) {
                        attempts++;

                        // Случайные координаты в пределах секции
                        int x = sx * sectionWidth + random.nextInt(sectionWidth);
                        int y = sy * sectionHeight + random.nextInt(sectionHeight);

                        // Проверяем границы
                        if (x >= width || y >= height) continue;

                        // Проверяем, что клетка свободна
                        if (!gameGrid[x][y].isEmpty()) continue;

                        WorldTile worldTile = getWorldTile(x, y);
                        GameplayUnitsType type = types[random.nextInt(types.length)];

                        // Особые условия для портов
                        if (type == GameplayUnitsType.PORT) {
                            if (!hasWaterNeighbor(x, y)) {
                                // Если порт не может быть здесь, выбираем другой тип
                                type = types[(random.nextInt(types.length - 1))];
                            }
                        }

                        // Создаем и размещаем объект
                        GameplayUnits obj = new GameplayUnits(this, x, y, type);
                        if(!worldTile.isWater()) addGameplayUnits(obj);
                        generatedCount++;
                        placed = true;
                    }
                }
            }
        }

        // Дополнительно уменьшаем плотность, удаляя часть объектов
        if (generatedCount > count * 0.7) {
            int toRemove = generatedCount - (int)(count * 0.7);
            for (int i = 0; i < toRemove && !gameplayUnits.isEmpty(); i++) {
                int index = random.nextInt(gameplayUnits.size());
                GameplayUnits obj = gameplayUnits.get(index);
                removeGameplayUnits(obj);
                generatedCount--;
            }
        }
    }
    /**
     * Рассчитывает стоимость содержания всех станций
     * @return Общая стоимость содержания станций
     */
    public float calculateStationsUpkeep() {
        float totalUpkeep = 0;

        for (Station station : stations) {
            // Пропускаем строящиеся/разрушающиеся станции
            if (station.getType() == StationType.BUILDING ||station.getType() == StationType.PLANNED ||
                    station.getType() == StationType.DESTROYED) {
                continue;
            }

            WorldTile tile = getWorldTile(station.getX(), station.getY());
            float perm = tile.getPerm();

            // Формула: базовое содержание * (1 + perm)
            // Чем выше perm (тверже порода), тем дороже содержание
            totalUpkeep += GameConstants.BASE_STATION_UPKEEP * (1 + perm);
        }

        return totalUpkeep;
    }

    /**
     * Рассчитывает стоимость содержания всех туннелей
     * @return Общая стоимость содержания туннелей
     */
    public float calculateTunnelsUpkeep() {
        float totalUpkeep = 0;

        for (Tunnel tunnel : tunnels) {
            // Пропускаем строящиеся/разрушающиеся туннели
            if (tunnel.getType() == TunnelType.BUILDING ||tunnel.getType() == TunnelType.PLANNED ||
                    tunnel.getType() == TunnelType.DESTROYED) {
                continue;
            }

            // Для каждого сегмента туннеля рассчитываем стоимость
            for (PathPoint point : tunnel.getPath()) {
                WorldTile tile = getWorldTile(point.getX(), point.getY());
                float perm = tile.getPerm();

                // Формула: базовое содержание * (1 + perm) за каждый сегмент
                totalUpkeep += GameConstants.BASE_TUNNEL_UPKEEP_PER_SEGMENT * (1 + perm);
            }
        }

        return totalUpkeep;
    }

    /**
     * Вычитает стоимость содержания из бюджета
     * @return true если денег хватило, false если бюджет ушел в минус
     */
    public boolean deductUpkeepCosts() {
        float stationsCost = calculateStationsUpkeep();
        float tunnelsCost = calculateTunnelsUpkeep();
        float totalCost = stationsCost + tunnelsCost;
        return removeMoney(totalCost);
    }
    // Проверяет, есть ли вода в соседних клетках
    private boolean hasWaterNeighbor(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue; // Пропускаем центральную клетку

                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (getWorldTile(nx, ny).isWater()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    @Override
    public World getWorld() {
        return this;
    }
    public float getMoney() {
        return money;
    }

    public boolean canAfford(float amount) {
        return money >= amount;
    }
    public boolean removeMoney(float amount) {
        if (amount < 0 && !canAfford(-amount)) {
            return false;
        }
        money -= amount;

        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().updateMoneyDisplay(money);
        }

        return true;
    }
    public boolean addMoney(float amount) {
        if (amount < 0 && !canAfford(-amount)) {
            return false;
        }
        money += amount;

        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().updateMoneyDisplay(money);
        }

        return true;
    }

    public void setMoney(int amount) {
        this.money = amount;
        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().updateMoneyDisplay(money);
        }
    }
    public void updateConnectedTunnels(Station station) {
        List<Tunnel> tunnelsToUpdate = new ArrayList<>(this.getTunnels());

        for (Tunnel tunnel : tunnelsToUpdate) {
            if (tunnel.getStart() == station || tunnel.getEnd() == station) {
                Station otherEnd = (tunnel.getStart() == station) ? tunnel.getEnd() : tunnel.getStart();

                // Туннель может начать строиться только если:
                // 1. Обе станции в BUILDING ИЛИ
                // 2. Одна в BUILDING, а другая уже построена (не PLANNED и не BUILDING)
                boolean canStartBuilding = false;

                // Обе станции в BUILDING
                boolean bothBuilding = station.getType() == StationType.BUILDING &&
                        otherEnd.getType() == StationType.BUILDING;

                // Одна в BUILDING, другая уже построена
                boolean oneBuildingOneBuilt =
                        (station.getType() == StationType.BUILDING &&
                                otherEnd.getType() != StationType.PLANNED &&
                                otherEnd.getType() != StationType.BUILDING) ||
                                (otherEnd.getType() == StationType.BUILDING &&
                                        station.getType() != StationType.PLANNED &&
                                        station.getType() != StationType.BUILDING);

                if ((bothBuilding || oneBuildingOneBuilt) &&
                        tunnel.getType() == TunnelType.PLANNED) {

                    float tunnelCost = tunnel.getLength() * GameConstants.TUNNEL_COST_PER_SEGMENT;
                    if (canAfford(tunnelCost)) {
                        addMoney(-tunnelCost);
                        tunnel.setType(TunnelType.BUILDING);
                        tunnel.getWorld().addTunnel(tunnel); // Обновит время строительства
                        canStartBuilding = true;
                    }
                }

                // Туннель становится ACTIVE только когда обе станции построены
                // (не PLANNED и не BUILDING)
                if (!canStartBuilding &&
                        station.getType() != StationType.PLANNED &&
                        station.getType() != StationType.BUILDING &&
                        otherEnd.getType() != StationType.PLANNED &&
                        otherEnd.getType() != StationType.BUILDING) {

                    tunnel.setType(TunnelType.ACTIVE);
                }
            }
        }
    }
    private void restoreStationConnections() {
        // Создаем карту станций по именам для быстрого доступа
        Map<String, Station> stationMap = new HashMap<>();
        for (Station station : this.stations) {
            stationMap.put(station.getName(), station);
        }

        // Восстанавливаем соединения
        for (Station station : this.stations) {
            for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
                Direction dir = entry.getKey();
                Station connectedTo = entry.getValue();

                // Обновляем ссылку на соединенную станцию
                Station actualConnectedTo = stationMap.get(connectedTo.getName());
                if (actualConnectedTo != null) {
                    station.getConnections().put(dir, actualConnectedTo);
                }
            }
        }

        // Дополнительно проверяем соседей для восстановления TRANSFER станций
        for (Station station : this.stations) {
            station.updateType();
        }
    }
public void initWorldGrid() {
    this.worldGrid = new WorldTile[width][height];
    this.gameGrid = new GameTile[width][height];
    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            worldGrid[x][y] = new WorldTile(x,y, 0, false, 0,0, Color.DARK_GRAY);
            gameGrid[x][y] = new GameTile(x,y);
        }
    }
}
    public void initGameGrid() {
        this.gameGrid = new GameTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                gameGrid[x][y] = new GameTile(x,y);
            }
        }

    }
    public void saveWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            serializer.saveWorld(this, SAVE_FILE);

            MetroLogger.logInfo("World successfully saved");
            MessageUtil.showTimedMessage(LngUtil.translatable("world.saved"), false, 2000);
        } catch (IOException ex) {
            MetroLogger.logError("Failed to save world", ex);
            MessageUtil.showTimedMessage(LngUtil.translatable("world.not_saved") + ex.getMessage(), true, 2000);
        }
    }

    public boolean loadWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            GameWorld loaded = serializer.loadWorld(SAVE_FILE);



            // Копируем данные из загруженного мира
            this.width = loaded.width;
            this.height = loaded.height;

            this.money = loaded.money;
            this.worldGrid = loaded.worldGrid;
            this.gameGrid = loaded.gameGrid;
            this.stations = loaded.stations;
            this.gameplayUnits = loaded.gameplayUnits;


            this.tunnels = loaded.tunnels;
            this.labels = loaded.labels;
            this.gameTime = loaded.gameTime;
            this.roundStationsEnabled = loaded.roundStationsEnabled;

            // Восстанавливаем временные данные
            this.stationBuildStartTimes = loaded.stationBuildStartTimes;
            this.stationBuildDurations = loaded.stationBuildDurations;
            this.tunnelBuildStartTimes = loaded.tunnelBuildStartTimes;
            this.tunnelBuildDurations = loaded.tunnelBuildDurations;
            this.stationDestructionStartTimes = loaded.stationDestructionStartTimes;
            this.stationDestructionDurations = loaded.stationDestructionDurations;
            this.tunnelDestructionStartTimes = loaded.tunnelDestructionStartTimes;
            this.tunnelDestructionDurations = loaded.tunnelDestructionDurations;

            restoreStationConnections();

            // Запускаем игровое время
            if (this.gameTime != null) {
                this.gameTime.start();
            } else {
                this.gameTime = new GameTime();
                this.gameTime.start();
            }

            if (this.screen != null) {
                this.screen.reinitializeControllers();
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    gameGrid[x][y].restoreContent(this);
                }
            }
            MetroLogger.logInfo("World successfully loaded");
            MessageUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
            return true;
        } catch (FileNotFoundException ex) {
            // Файл не найден - это нормально при первом запуске
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            MetroLogger.logError("Failed to load world", ex);
            MessageUtil.showTimedMessage(LngUtil.translatable("world.not_loaded") + ex.getMessage(), true, 2000);
        }
        return false;
    }

}
