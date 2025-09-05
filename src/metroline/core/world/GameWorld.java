package metroline.core.world;

import metroline.MainFrame;
import metroline.core.time.ConstructionTimeProcessor;
import metroline.core.time.GameTime;
import metroline.core.world.cities.CityManager;
import metroline.core.world.economic.EconomyManager;
import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.enums.*;
import metroline.objects.gameobjects.*;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;
import metroline.util.localizate.LngUtil;
import metroline.util.MetroLogger;
import metroline.util.PerlinNoise;
import metroline.util.VoronoiNoise;
import metroline.util.serialize.MetroSerializer;
import metroline.util.ui.UserInterfaceUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static metroline.core.world.economic.EconomyManager.stationRevenueAccumulator;

/**
 *
 */

public class GameWorld extends World {
    private transient MainFrame mainFrame;
    private static String SAVE_FILE = "game_save.metro";
    public float money;
    private MetroSerializer worldSerializer;
    private ConstructionTimeProcessor processor;
    public static boolean showGameplayUnits = false;
    public static boolean showPaymentZones = false;
    public static boolean showPassengerZones = false;
    public static boolean showGrassZones = false;

    private List<GameplayUnits> gameplayUnits = new CopyOnWriteArrayList<>();
    private List<Train> trains = new ArrayList<>();
    public long lastTrainsUpdateTime;

    private CityManager cityManager;
    private EconomyManager economyManager;

    private long lastZoneUpdate;

    public transient LinesLegendWindow legendWindow;
    public GameWorld() {
        super();

    }

    public GameWorld(short width, short height,boolean hasPassengerCount, boolean hasAbilityPay,  boolean hasLandscape, boolean hasRivers, int worldColor, int money) {
        super(null, width, height,worldColor, SAVE_FILE);
        this.mainFrame = MainFrame.getInstance();
        this.money = money;
        this.gameTime = new GameTime();
        processor = new ConstructionTimeProcessor(gameTime, this);
        processor.initTransientFields();

        this.economyManager = new EconomyManager(this);
        generateWorld(hasPassengerCount, hasAbilityPay, hasLandscape, hasRivers, worldColor);
        this.cityManager = new CityManager(this);
        this.lastTrainsUpdateTime = System.nanoTime();
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
        this.economyManager = new EconomyManager(this);


        worldSerializer.recreateWorld(reader, this);


    }

    public void generateWorld(boolean hasPassengerCount, boolean hasAbilityPay, boolean hasLandscape, boolean hasRivers, int worldColor) {
        initWorldGrid();
        MetroLogger.logInfo("Generating game world...");
        worldGrid = new WorldTile[width*height];
        gameGrid = new GameTile[width*height];
        gameplayGrid = new GameTile[width*height];
        // Инициализация генераторов шума с общим seed для согласованности
        long seed = rand.nextLong();
        WorldTile worldTile;
        GameTile gameTile;
        GameTile gameplayTile;

        PerlinNoise perlin = new PerlinNoise(seed);
        VoronoiNoise voronoi = new VoronoiNoise(seed);
        if(!hasLandscape) {
            for (short y = 0; y < height; y++) {
                for (short x = 0; x < width; x++) {
                    worldTile = new WorldTile(x, y, 0f, false, 0,0, 0x6E6E6E);
                    setWorldTile(x, y, worldTile);
                    worldTile.setBaseTileColor(worldColor);
                    gameTile = new GameTile(x, y);
                    setGameTile(x, y, gameTile);

                    gameplayTile = new GameTile(x, y);
                    setGameplayTile(x, y, gameplayTile);
                }
            }
        } else {

            for (short y = 0; y < height; y++) {
                for (short x = 0; x < width; x++) {
                    float nx = (float) x / width;
                    float ny = (float) y / height;

                    // Генерируем оба типа шума для одних и тех же координат
                    float perlinValue = perlin.fractalNoise(nx * 10, ny * 10, 0, 4, 0.5f);
                    float voronoiValue = voronoi.evaluate(nx * 15, ny * 15);

                    // Смешиваем шумы с учетом весов
                    float noiseValue = mixNoises(perlinValue, voronoiValue, 0.6f); // 60% перлина, 40% вороного

                    // Преобразуем в значение твердости породы (0..1)
                    float perm = transformNoiseToPerm(noiseValue);

                    worldTile = new WorldTile(x, y, perm, false, 0,0, 0xFFFFFF);
                    setWorldTile(x, y, worldTile);
                    worldTile.setBaseTileColor(worldColor);
                    gameTile = new GameTile(x, y);
                    setGameTile(x, y, gameTile);

                    gameplayTile = new GameTile(x, y);
                    setGameplayTile(x, y, gameplayTile);
                }
            }

        }




        if (hasRivers) {
            if(getWidth() > 300 || getHeight() > 300) {
                addRivers(rand.nextInt(1,9));
            } else
            if(getWidth() > 200 || getHeight() > 200) {
                addRivers(rand.nextInt(1,7));
            } else
            if(getWidth() > 100 || getHeight() > 100) {
                addRivers(rand.nextInt(1,5));
            } else
            if(getWidth() > 50 || getHeight() > 50) {
                addRivers(rand.nextInt(1,3));
            } else {
                addRivers(rand.nextInt(1,2));
            }
        }
        generateGrassLandscape();
        applyGradient();

        generateRandomGameplayUnits((int)GameConstants.GAMEPLAY_UNITS_COUNT);

        if(hasAbilityPay || hasPassengerCount) {
            updatePaymentAndPassengerZones();
        }
    }

    public void update() {

        economyManager.updateStationsWear();
        economyManager.processMaintenance();

        updateTrains();


        float collectedRevenue = economyManager.collectAllRevenue();

        if (collectedRevenue > 0) {
            addMoney(collectedRevenue);
        }

        updateCities();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastZoneUpdate > 60000) { // Каждую минуту
            updatePaymentAndPassengerZones();
            lastZoneUpdate = currentTime;
        }
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public void addTrainToStation(Station station, TrainType trainType) {
        if (station == null) return;

        Train train = new Train(this, station, trainType);
        trains.add(train);


        MetroLogger.logInfo("Train added to station: " + station.getName());
    }

    // Метод для удаления поезда
    public void removeTrain(Train train) {
        trains.remove(train);
    }

    public List<Train> getTrains() {
        return trains;
    }
    public void updateCities() {
        // Обновляем систему городка
        if (cityManager != null) {
            cityManager.update();
        }

        // Обновляем состояние всех игровых объектов
        updateGameplayUnits();
    }

        public void updateTrains() {
        long currentTime = System.nanoTime();
        long deltaTime = currentTime - lastTrainsUpdateTime;
        lastTrainsUpdateTime = currentTime;

        for (Train train : trains) {
            train.update(deltaTime);
        }
    }
    private void updateGameplayUnits() {
        for (GameplayUnits unit : gameplayUnits) {
            unit.updateCondition();
        }
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


    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
    }
    public void updateLegendWindow() {
        if (screen instanceof GameWorldScreen) {
            GameWorldScreen gameScreen = (GameWorldScreen) screen;
            if (gameScreen.parent != null && gameScreen.parent.legendWindow != null) {
                gameScreen.parent.legendWindow.updateLegend(this);
            }
        }
        if (screen instanceof SandboxWorldScreen) {
            SandboxWorldScreen gameScreen = (SandboxWorldScreen) screen;
            if (gameScreen.parent != null && gameScreen.parent.legendWindow != null) {
                gameScreen.parent.legendWindow.updateLegend(this);
            }
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

        // Используем EconomyManager для расчета стоимости строительства
        float constructionCost = economyManager.calculateStationConstructionCost(station.getX(), station.getY());

        if (!removeMoney(constructionCost)) {

            return;
        }

        if (station.getType() == StationType.BUILDING) {
            if (!getConstructionProcessor().getStationBuildStartTimes().containsKey(station)) {
                long startTime = gameTime.getCurrentTimeMillis();
                getConstructionProcessor().getStationBuildStartTimes().put(station, startTime);

                // Используем константы из EconomyManager
                long buildTime = (long) (getConstructionProcessor().getStationBuildTime() *
                        (1 + this.getWorldTile(station.getX(), station.getY()).getPerm()));
                getConstructionProcessor().getStationBuildDurations().put(station, buildTime);
            }
        }
    }
    @Override
    public void addTunnel(Tunnel tunnel) {
        super.addTunnel(tunnel);

        // Используем EconomyManager для расчета стоимости строительства
        float constructionCost = economyManager.calculateTunnelConstructionCost(tunnel);
        if (!removeMoney(constructionCost)) {
            MetroLogger.logInfo("Cannot afford tunnel construction: " + constructionCost);
            return;
        }

        if (getConstructionProcessor().getTunnelBuildStartTimes() == null) {
            getConstructionProcessor().setTunnelBuildStartTimes(new HashMap<>());
        }

        if (tunnel.getType() == TunnelType.BUILDING) {
            long startTime = gameTime.getCurrentTimeMillis();
            getConstructionProcessor().getTunnelBuildStartTimes().put(tunnel, startTime);

            // Используем EconomyManager для расчета времени строительства
            long buildTime = (long) (economyManager.calculateTunnelConstructionCost(tunnel) / 10); // Примерная формула
            getConstructionProcessor().getTunnelBuildDurations().put(tunnel, buildTime);
        }
    }
    public float getDemolitionCost(Station station) {
        // Используем EconomyManager для расчета стоимости сноса
        return economyManager.calculateStationRepairCost(station) * 0.5f; // Снос дешевле ремонта
    }

    public float getRepairCost(Station station) {
        // Используем EconomyManager для расчета стоимости ремонта
        return economyManager.calculateStationRepairCost(station);
    }
    public GameplayUnits getGameplayUnitsAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        GameObject obj = getGameplayTile(x, y).getContent();
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
        StationLabel stationLabel = this.getLabelAt(x, y);
        if (stationLabel != null) {
            return stationLabel;
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

    /*********************
     * ECONOMIC SECTION
     *********************/

    @Override
    public void removeStation(Station station) {
        super.removeStation(station);

    }
    public float getMoney() {
        return money;
    }
    public void deductMoney(float amount) {
        if (canAfford(amount)) {
            addMoney(-amount);
        }
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
            MainFrame.getInstance().mainFrameUI.updateMoneyDisplay(money);
        }

        return true;
    }
    public boolean addMoney(float amount) {
        if (amount < 0 && !canAfford(-amount)) {
            return false;
        }
        money += amount;

        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().mainFrameUI.updateMoneyDisplay(money);
        }

        return true;
    }

    public void setMoney(int amount) {
        this.money = amount;
        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().mainFrameUI.updateMoneyDisplay(money);
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
    public void generateGrassLandscape() {

        PerlinNoise perlin = new PerlinNoise(System.currentTimeMillis());
        VoronoiNoise voronoi = new VoronoiNoise(System.currentTimeMillis() + 1);

        float scale = 0.03f; // Уменьшим масштаб для более крупных features

        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                WorldTile tile = getWorldTile(x, y);
                if (tile != null && !tile.isWater()) {
                    // Многоканальный шум для большего разнообразия
                    float perlin1 = perlin.noise(x * scale, y * scale, 0);
                    float perlin2 = perlin.noise(x * scale * 2 + 100, y * scale * 2 + 100, 0);
                    float perlin3 = perlin.noise(x * scale * 4 + 200, y * scale * 4 + 200, 0);

                    float voronoi1 = voronoi.evaluate(x * scale * 0.8f, y * scale * 0.8f);
                    float voronoi2 = voronoi.evaluate(x * scale * 1.5f + 50, y * scale * 1.5f + 50);

                    // Комбинируем с усилением контраста
                    float combined = (perlin1 * 0.4f + perlin2 * 0.3f + perlin3 * 0.2f) * 0.9f +
                            (voronoi1 * 0.6f + voronoi2 * 0.4f) * 0.1f;

                    // Резкое увеличение контраста
                    combined = enhanceContrast(combined, 2.5f); // Сильный контраст

                    // Расширяем диапазон
                    combined = combined * 1.8f - 0.4f; // Сдвигаем и растягиваем
                    combined = Math.max(0, Math.min(1, combined));

                    // Нелинейное преобразование для большего визуального разнообразия
                    combined = (float) Math.pow(combined, 0.4); // Обратная гамма-коррекция

                    tile.setGrassValue(combined);

                    minValue = Math.min(minValue, combined);
                    maxValue = Math.max(maxValue, combined);
                } else if (tile != null) {
                    tile.setGrassValue(0); // Вода - нет травы
                }
            }
        }



        // Более агрессивное сглаживание
        smoothGrassLandscapeHighContrast();
    }

    private float enhanceContrast(float value, float contrast) {
        // Более мягкая функция контраста
        return (float) Math.tanh((value - 0.5f) * contrast * 1.5f) * 0.5f + 0.5f;
    }

    private void smoothGrassLandscapeHighContrast() {
        // Увеличиваем влияние сглаживания для большей мягкости
        for (int y = 2; y < getHeight() - 2; y++) {
            for (int x = 2; x < getWidth() - 2; x++) {
                WorldTile tile = getWorldTile(x, y);
                if (tile != null && !tile.isWater()) {
                    float sum = 0;
                    float totalWeight = 0;

                    // Gaussian kernel 5x5 (более мягкий)
                    float[][] weights = {
                            {0.005f, 0.02f, 0.03f, 0.02f, 0.005f},
                            {0.02f, 0.08f, 0.12f, 0.08f, 0.02f},
                            {0.03f, 0.12f, 0.18f, 0.12f, 0.03f},
                            {0.02f, 0.08f, 0.12f, 0.08f, 0.02f},
                            {0.005f, 0.02f, 0.03f, 0.02f, 0.005f}
                    };

                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            WorldTile neighbor = getWorldTile(x + dx, y + dy);
                            if (neighbor != null && !neighbor.isWater()) {
                                float weight = weights[dy + 2][dx + 2];
                                sum += neighbor.getGrassValue() * weight;
                                totalWeight += weight;
                            }
                        }
                    }

                    if (totalWeight > 0) {
                        // Больше сглаживания, меньше оригинальной текстуры
                        float smoothed = (tile.getGrassValue() * 0.5f) + (sum / totalWeight * 0.5f);
                        tile.setGrassValue(smoothed);
                    }
                }
            }
        }
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
            if (!getGameplayTile(x,y).isEmpty()) continue;

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

    public void addTrain(Train train) {
        trains.add(train);
    }
    public void addGameplayUnits(GameplayUnits obj) {
        gameplayUnits.add(obj);
        getGameplayTile(obj.getX(), obj.getY()).setContent(obj);

        // Обновляем зоны при добавлении здания
        updatePaymentAndPassengerZones();

        if (cityManager != null) {
            cityManager.onBuildingAdded(obj);
        }

        if (GameWorldScreen.getInstance() != null) {
            GameWorldScreen.getInstance().invalidateCache();
        }
    }

    public void removeGameplayUnits(GameplayUnits obj) {
        gameplayUnits.remove(obj);
        getGameplayTile(obj.getX(), obj.getY()).setContent(null);

        // Обновляем зоны при удалении здания
        updatePaymentAndPassengerZones();

        if (cityManager != null) {
            cityManager.onBuildingRemoved(obj);
        }

        if (GameWorldScreen.getInstance() != null) {
            GameWorldScreen.getInstance().invalidateCache();
        }
    }
    public CityManager getCityManager() {
        return cityManager;
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
    public void updatePaymentAndPassengerZones() {
        // Сначала сбрасываем значения
        resetZones();

        // Затем применяем влияние всех существующих зданий
        applyBuildingInfluence();

        // Обновляем кэш отображения
        invalidateZonesCache();
    }
    public void invalidateZonesCache() {
        if (GameWorldScreen.getInstance() != null) {
            GameWorldScreen screen = GameWorldScreen.getInstance();
            screen.invalidateZonesCache();

            // Принудительно обновляем кэш изображений
            int width = getWidth();
            int height = getHeight();

            if (screen.paymentZonesCache != null) {
                screen.updatePaymentZonesCache(width, height);
            }
            if (screen.passengerZonesCache != null) {
                screen.updatePassengerZonesCache(width, height);
            }

        }
    }
    private void resetZones() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                WorldTile tile = getWorldTile(x, y);
                if (tile != null) {
                    tile.setAbilityPay(0);
                    tile.setPassengerCount(0);
                }
            }
        }
    }

    private void applyBuildingInfluence() {
        // Проходим по всем GameplayUnits
        for (GameplayUnits unit : gameplayUnits) {
            if (unit.getType().isPassengerBuilding()) {
                // Жилые здания создают пассажиропоток
                applyPassengerInfluence(unit);
            } else {
                // Нежилые здания создают платежеспособность
                applyPaymentInfluence(unit);
            }
        }
    }

    private void applyPassengerInfluence(GameplayUnits unit) {
        int centerX = unit.getX();
        int centerY = unit.getY();
        float maxInfluence = unit.getType().getPassengerGeneration(); // Добавим этот метод в GameplayUnitsType

        // Радиус влияния (можно настроить)
        int radius = unit.getType().getInfluenceRadius();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float influence = calculateInfluence(maxInfluence, distance, radius);

                    WorldTile tile = getWorldTile(x, y);
                    if (tile != null) {
                        tile.setPassengerCount(tile.getPassengerCount() + (int) influence);
                    }
                }
            }
        }
    }

    private void applyPaymentInfluence(GameplayUnits unit) {
        int centerX = unit.getX();
        int centerY = unit.getY();
        float maxInfluence = unit.getType().getPaymentGeneration(); // Добавим этот метод в GameplayUnitsType

        // Радиус влияния
        int radius = unit.getType().getInfluenceRadius();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float influence = calculateInfluence(maxInfluence, distance, radius);

                    WorldTile tile = getWorldTile(x, y);
                    if (tile != null) {
                        tile.setAbilityPay(tile.getAbilityPay() + influence);
                    }
                }
            }
        }
    }

    private float calculateInfluence(float maxInfluence, float distance, int radius) {
        // Увеличиваем радиус влияния на 20%
        int extendedRadius = (int) (radius * 1.2f);
        if (distance > extendedRadius) return 0;

        // Нелинейное затухание с элементами хаоса
        float normalizedDistance = distance / extendedRadius;

        // Квадратичное затухание (более резкое в конце)
        float quadraticAttenuation = 1 - normalizedDistance * normalizedDistance;

        // Добавляем немного хаотичности (псевдо-случайные колебания)
        float chaosFactor = 0.15f; // Сила хаотичности (0-1)
        float randomVariation = (float) (1 - chaosFactor + Math.random() * chaosFactor * 2);

        // Комбинируем затухания с хаотическим фактором
        float attenuation = quadraticAttenuation * randomVariation;

        // Гарантируем, что влияние не станет отрицательным
        attenuation = Math.max(0, Math.min(1, attenuation));

        return maxInfluence * attenuation;
    }
    public void saveWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            serializer.saveWorld(this, SAVE_FILE);

            MetroLogger.logInfo("World successfully saved");
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.saved"), false, 2000);
        } catch (IOException ex) {
            MetroLogger.logError("Failed to save world", ex);
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.not_saved") + ex.getMessage(), true, 2000);
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
            this.gameGrid = loadedWorld.gameGrid;
            this.gameplayGrid = loadedWorld.gameplayGrid;  // Ссылка
            this.trains = loadedWorld.trains;
            this.stations = loadedWorld.stations;   // Ссылка
            this.gameplayUnits = loadedWorld.gameplayUnits; // Ссылка
            this.tunnels = loadedWorld.tunnels;     // Ссылка
            this.stationLabels = loadedWorld.stationLabels;       // Ссылка
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

            this.economyManager = new EconomyManager(this);
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
            updatePaymentAndPassengerZones();
            MetroLogger.logInfo("World successfully loaded");
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
            return true;
        } catch (java.io.FileNotFoundException ex) {
            // Файл не найден - это нормально при первом запуске
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            MetroLogger.logError("Failed to load world", ex);
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.not_loaded") + ex.getMessage(), true, 2000);
        }
        return false;
    }

    // Метод для получения содержания станции для отображения в UI
    public float getStationUpkeep(Station station) {
        return economyManager.calculateStationUpkeep(station);
    }

    // Метод для получения содержания туннеля для отображения в UI
    public float getTunnelUpkeep(Tunnel tunnel) {
        return economyManager.calculateTunnelUpkeep(tunnel);
    }

}
