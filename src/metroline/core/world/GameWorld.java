package metroline.core.world;

import metroline.core.time.ConstructionTimeProcessor;
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
import metroline.util.*;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO ПОПРАВИТЬ ВОЗМОЖНОСТЬ РАСПОЛОЖЕНИЯ МЕТОК
 */

public class GameWorld extends World {
    private transient MainFrame mainFrame;
    private static String SAVE_FILE = "game_save.metro";
    public float money;
    private MetroSerializer worldSerializer;
    private ConstructionTimeProcessor processor;

    private List<GameplayUnits> gameplayUnits = new ArrayList<>();

    public transient LinesLegendWindow legendWindow;
    public GameWorld() {
        super();

    }

    public GameWorld(int width, int height,boolean hasPassengerCount, boolean hasAbilityPay,  boolean hasLandscape, boolean hasRivers, Color worldColor, int money) {
        super(null, width, height, hasPassengerCount, hasAbilityPay, hasLandscape,hasRivers,worldColor, SAVE_FILE);
        this.mainFrame = MainFrame.getInstance();
        this.money = money;
        this.gameTime = new GameTime();
        processor = new ConstructionTimeProcessor(gameTime, this);
        processor.initTransientFields();
        generateWorld(hasPassengerCount, hasAbilityPay, hasLandscape, hasRivers, worldColor);

    }
    /**
     * Создает новый GameWorld, загружая его состояние из BufferedReader.
     * Этот конструктор предназначен для использования сериализатором.
     *
     * @param reader BufferedReader, откуда будут читаться данные.
     * @throws IOException Если возникает ошибка при чтении.
     */
    public GameWorld(BufferedReader reader) throws IOException {
        super(); // Вызываем базовый конструктор World()

        this.mainFrame = MainFrame.getInstance();
        processor = new ConstructionTimeProcessor(gameTime, this);
        processor.initTransientFields(); // Инициализируем transient поля
        worldSerializer = new MetroSerializer();
        worldSerializer.recreateWorld(reader, this);


    }

    public void generateWorld(boolean hasPassengerCount, boolean hasAbilityPay, boolean hasLandscape, boolean hasRivers, Color worldColor) {
        //Create world grid
        System.out.println("Generating world...");
        worldGrid = new WorldTile[width][height];
        gameGrid = new GameTile[width][height];
        // Инициализация генераторов шума с общим seed для согласованности
        long seed = rand.nextLong();
        PerlinNoise perlin = new PerlinNoise(seed);
        VoronoiNoise voronoi = new VoronoiNoise(seed);
        if(!hasLandscape) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    worldGrid[x][y] = new WorldTile(x, y, 0f, false, 0,0, Color.DARK_GRAY);
                    worldGrid[x][y].setBaseTileColor(worldColor);
                    gameGrid[x][y] = new GameTile(x, y);
                }
            }
        } else {

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float nx = (float) x / width;
                    float ny = (float) y / height;

                    // Генерируем оба типа шума для одних и тех же координат
                    float perlinValue = perlin.fractalNoise(nx * 10, ny * 10, 0, 4, 0.5f);
                    float voronoiValue = voronoi.evaluate(nx * 15, ny * 15);

                    // Смешиваем шумы с учетом весов
                    float noiseValue = mixNoises(perlinValue, voronoiValue, 0.6f); // 60% перлина, 40% вороного

                    // Преобразуем в значение твердости породы (0..1)
                    float perm = transformNoiseToPerm(noiseValue);

                    // Создаем тайл
                    worldGrid[x][y] = new WorldTile(x, y, perm, false, 0,0, Color.WHITE);
                    worldGrid[x][y].setBaseTileColor(worldColor);
                    gameGrid[x][y] = new GameTile(x, y);

                }
            }

        }
        // Генерация зон с использованием того же смешанного шума
        if(hasAbilityPay) {
            generatePaymentZones(perlin, voronoi);
        }
        if(hasPassengerCount) {
            generatePassengerZones(perlin, voronoi);
        }



        if (hasRivers) {
            if(getWidth() > 100 || getHeight() > 100) {
                addRivers(rand.nextInt(2,8));
            } else
            if(getWidth() > 50 || getHeight() > 50) {
                addRivers(rand.nextInt(3,5));
            } else {
                addRivers(rand.nextInt(1,2));
            }
        }

        applyGradient();
    }

    private float mixNoises(float perlin, float voronoi, float perlinWeight) {
        // Нормализуем вороной шум (изначально 0..1)
        voronoi = (float)Math.pow(voronoi, 2);

        // Смешиваем с весами
        return perlin * perlinWeight + voronoi * (1 - perlinWeight);
    }

    private float transformNoiseToPerm(float noise) {
        // Преобразуем шум в значение твердости породы
        // Здесь можно настроить кривую распределения
        if (noise < 0.3f) {
            return 0.1f + noise * 0.6f; // Мягкие породы
        } else if (noise < 0.7f) {
            return 0.4f + (noise - 0.3f) * 0.2f; // Средние породы
        } else {
            return 0.9f + (noise - 0.7f) * 0.3f; // Твердые породы
        }
    }
    private void generatePaymentZones(PerlinNoise perlin, VoronoiNoise voronoi) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float nx = (float)x / width * 12f; // Более высокая частота
                float ny = (float)y / height * 12f;

                // Больше вороного для четких зон
                float perlinValue = perlin.noise(nx, ny, 0);
                float voronoiValue = voronoi.evaluate(nx, ny);

                //   value = 1f - value; // Инвертируем

                if (voronoiValue > 0.4f) { // Более высокий порог
                    worldGrid[x][y].setAbilityPay((float) (voronoiValue * 1.5)); // Усиливаем значения
                }
            }
        }
    }

    private void generatePassengerZones(PerlinNoise perlin, VoronoiNoise voronoi) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float nx = (float)x / width * 12f; // Более высокая частота
                float ny = (float)y / height * 12f;

                // Больше вороного для четких зон
                float perlinValue = perlin.noise(nx, ny, 0);
                float voronoiValue = voronoi.evaluate(nx, ny);
                float value = mixNoises(perlinValue, voronoiValue, 0.3f); // 70% вороного

                value = 1f - value; // Инвертируем

                if (value > 0.6f) { // Только самые яркие зоны
                    worldGrid[x][y].setPassengerCount((int)(value * 1200)); // Усиливаем значения
                }
            }
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
            if (!getConstructionProcessor().getStationBuildStartTimes().containsKey(station)) {
                long startTime = gameTime.getCurrentTimeMillis();
                getConstructionProcessor().getStationBuildStartTimes().put(station, startTime);
                getConstructionProcessor().getStationBuildDurations().put(station, (long) (getConstructionProcessor().getStationBuildTime()*(1+this.getWorldTile(station.getX(), station.getY()).getPerm())));

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
        if (getConstructionProcessor().getTunnelBuildStartTimes() == null) {

            getConstructionProcessor().setTunnelBuildStartTimes(new HashMap<>());
        }
        if (getConstructionProcessor().getTunnelBuildDurations() == null) {

            getConstructionProcessor().setTunnelBuildDurations(new HashMap<>());
        }

        if (tunnel.getType() == TunnelType.BUILDING) {

            long startTime = gameTime.getCurrentTimeMillis();
            getConstructionProcessor().getTunnelBuildStartTimes().put(tunnel, startTime);
            getConstructionProcessor().getTunnelBuildDurations().put(tunnel, getConstructionProcessor().getTunnelBuildTime());

            // Устанавливаем стоимость строительства в зависимости от длины
            int lengthBasedCost = tunnel.getLength() * 10; // Например, 10 за сегмент
            getConstructionProcessor().setTunnelBuildTime(Math.max(50000, lengthBasedCost * 1000)); // Минимум 50 секунд

        }

    }
    public GameplayUnits getGameplayUnitsAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        GameObject obj = gameGrid[x][y].getContent();
        return obj instanceof GameplayUnits ? (GameplayUnits)obj : null;
    }



    public void startDestroyingTunnel(Tunnel tunnel) {
        if (tunnel.getType() != TunnelType.ACTIVE) {
            MetroLogger.logWarning("Cannot destroy tunnel of type " + tunnel.getType());
            return;
        }

        tunnel.setType(TunnelType.DESTROYED);
        long startTime = gameTime.getCurrentTimeMillis();
        getConstructionProcessor().getTunnelDestructionStartTimes().put(tunnel, startTime);
        getConstructionProcessor().getTunnelDestructionDurations().put(tunnel, getConstructionProcessor().getTunnelDestroyTime());

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
        getConstructionProcessor().getStationDestructionStartTimes().put(station, startTime);

        getConstructionProcessor().getStationDestructionDurations().put(station, getConstructionProcessor().getstationDestroyTime());
        for (Tunnel tunnel : new ArrayList<>(tunnels)) {
            if (tunnel.getStart() == station || tunnel.getEnd() == station) {
                startDestroyingTunnel(tunnel);
            }
        }

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

    public GameTime getGameTime() {
        return gameTime;
    }




    public ConstructionTimeProcessor getConstructionProcessor() {
        return processor;
    }
    public float calculateStationsUpkeep() {
        float totalUpkeep = 0;
        for (Station station : getStations()) {
            totalUpkeep += station.calculateUpkeepCost();
        }
        return totalUpkeep;
    }
    public float calculateTunnelsUpkeep() {
        float totalUpkeep = 0;
        for (Tunnel t : getTunnels()) {
            totalUpkeep = t.calculateTunnelsUpkeep();

        }
        return totalUpkeep;
    }
    /*********************
     * ECONOMIC SECTION
     *********************/
    public float calculateStationRevenue(Station station) {
        // Получаем тайл, на котором стоит станция
        WorldTile tile = getWorldTile(station.getX(), station.getY());

        // Базовый доход
        float revenue = GameConstants.STATION_BASE_REVENUE;

        float permModifier = 1 + (tile.getPerm());

        float abilityPayModifier = 1 + (tile.getAbilityPay());

        float passengerModifier = 1 + (float) tile.getPassengerCount() / 2000;

        // Итоговый расчет
        revenue = revenue * permModifier * abilityPayModifier * passengerModifier;
        float gameplayUnitsMultiplier = 1.0f;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = station.getX() + dx;
                int ny = station.getY() + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    GameObject obj = gameGrid[nx][ny].getContent();
                    if (obj instanceof GameplayUnits) {
                        gameplayUnitsMultiplier *= ((GameplayUnits) obj).getType().getIncomeMultiplier();


                    }
                }
            }
        }

        float wearModifier = 1f - station.getWearLevel() * 0.5f; // Доход падает до 50% при максимальном износе

        if (station.getType() == StationType.DROWNED ||
                station.getType() == StationType.ABANDONED ||
                station.getType() == StationType.BURNED ||
                station.getType() == StationType.RUINED ||
                station.getType() == StationType.BUILDING ||
                station.getType() == StationType.CLOSED ||
                station.getType() == StationType.DESTROYED ||
                station.getType() == StationType.PLANNED) {
            return 0;
        }
        return revenue * gameplayUnitsMultiplier * wearModifier;
    }
    public float getDemolitionCost(Station station) {
        float baseCost = GameConstants.BASE_STATION_DEMOLITION_COST;

        if (station.getType() == StationType.RUINED ||
                station.getType() == StationType.ABANDONED) {
            return baseCost * (1 + station.getWearLevel()) * 3f; // В 3 раза дороже
        }

        return baseCost * (1 + station.getWearLevel());
    }
    public void updateStationsWear() {
        for (Station station : stations) {
            station.updateWear();
        }
    }
    public void updateStationsRevenue() {
        for (Station station : stations) {
            // Пропускаем станции, которые строятся или разрушаются
            if (station.isLowIncomeStations()) {
                addMoney(0);
                return;
            }

            float revenue = calculateStationRevenue(station);
            addMoney(revenue);
        }
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
    public void generateRandomGameplayUnits(int count) {
        GameplayUnitsType[] types = GameplayUnitsType.values();
        int generatedCount = 0;
        int attempts = 0;
        int maxAttempts = count * 10; // Максимальное количество попыток

        while (generatedCount < count && attempts < maxAttempts) {
            attempts++;

            // Случайные координаты по всей карте
            int x = ThreadLocalRandom.current().nextInt(width);
            int y = ThreadLocalRandom.current().nextInt(height);

            // Проверяем, что клетка свободна
            if (!gameGrid[x][y].isEmpty()) continue;

            WorldTile worldTile = getWorldTile(x, y);
            GameplayUnitsType type = types[ThreadLocalRandom.current().nextInt(types.length)];

            // Особые условия для портов
            if (type == GameplayUnitsType.PORT) {
                if (!hasWaterNeighbor(x, y)) {
                    // Если порт не может быть здесь, выбираем другой тип
                    type = types[ThreadLocalRandom.current().nextInt(types.length - 1)];
                }
            }

            // Создаем и размещаем объект (только на суше)
            if (!worldTile.isWater()) {
                GameplayUnits obj = new GameplayUnits(this, x, y, type);
                addGameplayUnits(obj);
                generatedCount++;
            }
        }

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
    @Override
    public World getWorld() {
        return this;
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
                worldGrid[x][y] = new WorldTile(x,y);
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
    /**
     * Загружает мир из стандартного файла сохранения.
     * Делегирует загрузку статическому методу MetroSerializer.
     * @return true если загрузка успешна, false если файл не найден.
     */
    public boolean loadWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            // MetroSerializer.loadWorld теперь возвращает полностью сконструированный GameWorld
            GameWorld loadedWorld = serializer.loadWorld(SAVE_FILE);

            // Копируем ссылки на данные из загруженного мира в текущий экземпляр
            // Это предполагает, что текущий экземпляр GameWorld заменяется загруженным
            // Если нужно обновить *этот* экземпляр, логика должна быть другой
            // Например, можно сделать loadWorld статическим и возвращать GameWorld

            // Альтернатива: если loadWorld должен обновить *этот* экземпляр,
            // нужно скопировать все поля из loadedWorld в this.
            // Это менее элегантно, но возможно.

            // Пример копирования полей (предполагая, что сетки уже инициализированы):
            this.width = loadedWorld.width;
            this.height = loadedWorld.height;
            this.money = loadedWorld.money;
            this.worldGrid = loadedWorld.worldGrid; // Ссылка
            this.gameGrid = loadedWorld.gameGrid;   // Ссылка
            this.stations = loadedWorld.stations;   // Ссылка
            this.gameplayUnits = loadedWorld.gameplayUnits; // Ссылка
            this.tunnels = loadedWorld.tunnels;     // Ссылка
            this.labels = loadedWorld.labels;       // Ссылка
            this.gameTime = loadedWorld.gameTime;   // Ссылка
            this.roundStationsEnabled = loadedWorld.roundStationsEnabled;
            this.processor = new ConstructionTimeProcessor(this.gameTime, this);
            // Копируем transient поля
            getConstructionProcessor().stationBuildStartTimes = new HashMap<>(loadedWorld.getConstructionProcessor().stationBuildStartTimes);
            getConstructionProcessor().stationBuildDurations = new HashMap<>(loadedWorld.getConstructionProcessor().stationBuildDurations);
            getConstructionProcessor().tunnelBuildStartTimes = new HashMap<>(loadedWorld.getConstructionProcessor().tunnelBuildStartTimes);
            getConstructionProcessor().tunnelBuildDurations = new HashMap<>(loadedWorld.getConstructionProcessor().tunnelBuildDurations);
            getConstructionProcessor().stationDestructionStartTimes = new HashMap<>(loadedWorld.getConstructionProcessor().stationDestructionStartTimes);
            getConstructionProcessor().stationDestructionDurations = new HashMap<>(loadedWorld.getConstructionProcessor().stationDestructionDurations);
            getConstructionProcessor().tunnelDestructionStartTimes = new HashMap<>(loadedWorld.getConstructionProcessor().tunnelDestructionStartTimes);
            getConstructionProcessor().tunnelDestructionDurations = new HashMap<>(loadedWorld.getConstructionProcessor().tunnelDestructionDurations);

            // Восстанавливаем связи между объектами
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

            MetroLogger.logInfo("World successfully loaded");
            MessageUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
            return true;
        } catch (java.io.FileNotFoundException ex) {
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
