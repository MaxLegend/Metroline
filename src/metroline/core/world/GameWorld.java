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

    private long stationDestroyTime = 1000000; // Время разрушения станции
    private long tunnelDestroyTime = 1000000;
    private long stationBuildTime = 1000000;
    private long tunnelBuildTime = 1000000;

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

        generateWorld(hasPassengerCount, hasAbilityPay, hasLandscape, hasRivers, worldColor);

        initTransientFields();

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
        initTransientFields(); // Инициализируем transient поля

        String version = null;
        String line;
        // Map<String, Station> stationMap = new HashMap<>(); // Для обратной совместимости по имени, больше не используется
        Map<Long, Station> stationIdMap = new HashMap<>();

        while ((line = reader.readLine()) != null) {
            line = line.trim(); // Убираем пробелы в начале и конце

            if (line.isEmpty()) continue; // Пропускаем пустые строки

            if (line.startsWith("version:")) {
                version = line.substring("version:".length());
                continue;
            }
            if (line.startsWith("width:")) {
                this.width = Integer.parseInt(line.substring("width:".length()));
                continue; // Продолжаем, чтобы не попасть в else if
            }
            if (line.startsWith("height:")) {
                this.height = Integer.parseInt(line.substring("height:".length()));
                // Как только получили width и height, инициализируем сетки
                if (this.width > 0 && this.height > 0) {
                    initWorldGrid(); // Инициализируем worldGrid
                    initGameGrid();  // Инициализируем gameGrid
                }
                continue;
            }
            if (line.startsWith("money:")) {
                this.money = (int) Float.parseFloat(line.substring("money:".length()));
                continue;
            }
            if (line.startsWith("roundStations:")) {
                this.roundStationsEnabled = Boolean.parseBoolean(line.substring("roundStations:".length()));
                continue;
            }
            if (line.startsWith("gameTime:")) {
                long time = Long.parseLong(line.substring("gameTime:".length()));
                this.gameTime = new GameTime();
                // Предполагается, что GameTime может быть инициализирован напрямую или имеет сеттер
                // gameTime.setCurrentTime(time); // Если такой метод существует
                continue;
            }

            // --- Обработка секций данных ---

            if (line.equals("worldGrid:[")) {
                // Чтение worldGrid
                while (!(line = reader.readLine()).equals("]")) {
                    line = line.trim();
                    if (!line.startsWith("{") || !line.endsWith("}")) continue;
                    String content = line.substring(1, line.length() - 1);

                    // Используем регулярное выражение для корректного разбиения
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 6) {
                        System.err.println("Недостаточно частей при парсинге worldGrid: " + line);
                        continue;
                    }

                    try {
                        int x = Integer.parseInt(ParsingUtils.extractValue(parts[0], "x"));
                        int y = Integer.parseInt(ParsingUtils.extractValue(parts[1], "y"));
                        float perm = Float.parseFloat(ParsingUtils.extractValue(parts[2], "perm"));
                        boolean isWater = Boolean.parseBoolean(ParsingUtils.extractValue(parts[3], "isWater"));
                        float abilityPay = Float.parseFloat(ParsingUtils.extractValue(parts[4], "abilityPay"));
                        int passengerCount = Integer.parseInt(ParsingUtils.extractValue(parts[5], "passengerCount").split("\\.")[0]); // Убираем .00

                        WorldTile tile = this.getWorldTile(x, y);
                        if (tile != null) {
                            tile.setPerm(perm);
                            tile.setAbilityPay(abilityPay);
                            tile.setWater(isWater);
                            tile.setPassengerCount(passengerCount);
                            // tile.setBaseTileColor(color); // Если цвет сохраняется
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при парсинге worldGrid: " + line);
                        e.printStackTrace();
                    }
                }
                continue;
            }

            if (line.equals("stations:[")) {
                // Чтение станций
                while (!(line = reader.readLine()).equals("]")) {
                    Station station = ParsingUtils.parseStation(line, this); // Используем вспомогательный метод
                    if (station != null) {
                        // stationMap.put(station.getName(), station); // Больше не используется
                        stationIdMap.put(station.getUniqueId(), station);
                        this.stations.add(station); // Добавляем в список станций мира
                    }
                }
                continue;
            }

            if (line.equals("connections:[")) {
                // Чтение соединений
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConnection(line, this, stationIdMap); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("tunnels:[")) {
                // Чтение туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    Tunnel tunnel = ParsingUtils.parseTunnel(line, this, stationIdMap); // Используем вспомогательный метод
                    if (tunnel != null) {
                        this.tunnels.add(tunnel); // Добавляем в список туннелей мира
                    }
                }
                continue;
            }

            if (line.equals("gameplay_units:[")) {
                // Чтение игровых юнитов
                while (!(line = reader.readLine()).equals("]")) {
                    GameplayUnits gUnits = ParsingUtils.parseGameplayUnit(line, this); // Используем вспомогательный метод
                    if (gUnits != null) {
                        this.gameplayUnits.add(gUnits); // Добавляем в список юнитов мира
                        // Также нужно установить контент в gameGrid, если это необходимо
                        // gameGrid[gUnits.getX()][gUnits.getY()].setContent(gUnits);
                    }
                }
                continue;
            }

            if (line.equals("pathPoints:[")) {
                // Чтение путевых точек для туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parsePathPoints(line, this, stationIdMap); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("labels:[")) {
                // Чтение меток
                while (!(line = reader.readLine()).equals("]")) {
                    Label label = ParsingUtils.parseLabel(line, this, stationIdMap); // Используем вспомогательный метод
                    if (label != null) {
                        this.labels.add(label); // Добавляем в список меток мира
                        // Также нужно установить контент в gameGrid, если это необходимо
                        // gameGrid[label.getX()][label.getY()].setContent(label);
                    }
                }
                continue;
            }

            if (line.equals("stationBuild:[")) {
                // Чтение данных о строительстве станций
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, this, stationIdMap, true, false); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("stationDestroy:[")) {
                // Чтение данных о разрушении станций
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, this, stationIdMap, true, true); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("tunnelBuild:[")) {
                // Чтение данных о строительстве туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, this, stationIdMap, false, false); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("tunnelDestroy:[")) {
                // Чтение данных о разрушении туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, this, stationIdMap, false, true); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("gameGrid:[")) {
                // Чтение gameGrid (содержимое клеток)
                while (!(line = reader.readLine()).equals("]")) {
                    line = line.trim();
                    if (!line.startsWith("{") || !line.endsWith("}")) continue;
                    String content = line.substring(1, line.length() - 1);
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 3) {
                        System.err.println("Недостаточно частей при парсинге gameGrid: " + line);
                        continue;
                    }

                    try {
                        int x = Integer.parseInt(ParsingUtils.extractValue(parts[0], "x"));
                        int y = Integer.parseInt(ParsingUtils.extractValue(parts[1], "y"));
                        String contentStr = ParsingUtils.extractValue(parts[2], "content");

                        if (!contentStr.equals("null")) {
                            GameObject obj = ParsingUtils.parseGameObject(contentStr, this, stationIdMap); // Используем вспомогательный метод
                            if (obj != null && this.gameGrid != null && x < this.width && y < this.height) {
                                this.gameGrid[x][y].setContent(obj);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при парсинге gameGrid: " + line);
                        e.printStackTrace();
                    }
                }
                continue;
            }
        }

        // Восстанавливаем связи между объектами, если это необходимо
        restoreStationConnections();
        // initTransientFields(); // Уже вызвано в начале

        MetroLogger.logInfo("GameWorld successfully constructed from reader.");
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
        return revenue * gameplayUnitsMultiplier;
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

        if (generatedCount < count) {
            System.out.println("Не удалось разместить все объекты. Размещено: " + generatedCount + " из " + count);
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
        System.out.println("Init world grid");

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

            // Копируем transient поля
            this.stationBuildStartTimes = new HashMap<>(loadedWorld.stationBuildStartTimes);
            this.stationBuildDurations = new HashMap<>(loadedWorld.stationBuildDurations);
            this.tunnelBuildStartTimes = new HashMap<>(loadedWorld.tunnelBuildStartTimes);
            this.tunnelBuildDurations = new HashMap<>(loadedWorld.tunnelBuildDurations);
            this.stationDestructionStartTimes = new HashMap<>(loadedWorld.stationDestructionStartTimes);
            this.stationDestructionDurations = new HashMap<>(loadedWorld.stationDestructionDurations);
            this.tunnelDestructionStartTimes = new HashMap<>(loadedWorld.tunnelDestructionStartTimes);
            this.tunnelDestructionDurations = new HashMap<>(loadedWorld.tunnelDestructionDurations);

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
//    public boolean loadWorld() {
//        try {
//            MetroSerializer serializer = new MetroSerializer();
//            GameWorld loaded = serializer.loadWorld(SAVE_FILE);
//
//
//
//            // Копируем данные из загруженного мира
//            this.width = loaded.width;
//            this.height = loaded.height;
//
//            this.money = loaded.money;
//            this.worldGrid = loaded.worldGrid;
//
//       //     this.gameGrid = loaded.gameGrid;
//
//            this.stations = loaded.stations;
//            this.gameplayUnits = loaded.gameplayUnits;
//
//
//            this.tunnels = loaded.tunnels;
//            this.labels = loaded.labels;
//            this.gameTime = loaded.gameTime;
//            this.roundStationsEnabled = loaded.roundStationsEnabled;
//
//            // Восстанавливаем временные данные
//            this.stationBuildStartTimes = loaded.stationBuildStartTimes;
//            this.stationBuildDurations = loaded.stationBuildDurations;
//            this.tunnelBuildStartTimes = loaded.tunnelBuildStartTimes;
//            this.tunnelBuildDurations = loaded.tunnelBuildDurations;
//            this.stationDestructionStartTimes = loaded.stationDestructionStartTimes;
//            this.stationDestructionDurations = loaded.stationDestructionDurations;
//            this.tunnelDestructionStartTimes = loaded.tunnelDestructionStartTimes;
//            this.tunnelDestructionDurations = loaded.tunnelDestructionDurations;
//
//            restoreStationConnections();
//
//            // Запускаем игровое время
//            if (this.gameTime != null) {
//                this.gameTime.start();
//            } else {
//                this.gameTime = new GameTime();
//                this.gameTime.start();
//            }
//
//            if (this.screen != null) {
//                this.screen.reinitializeControllers();
//            }
//            for (int y = 0; y < height; y++) {
//                for (int x = 0; x < width; x++) {
//                    gameGrid[x][y].restoreContent(this);
//                }
//            }
//            MetroLogger.logInfo("World successfully loaded");
//            MessageUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
//            return true;
//        } catch (FileNotFoundException ex) {
//            // Файл не найден - это нормально при первом запуске
//            return false;
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            MetroLogger.logError("Failed to load world", ex);
//            MessageUtil.showTimedMessage(LngUtil.translatable("world.not_loaded") + ex.getMessage(), true, 2000);
//        }
//        return false;
//    }

}
