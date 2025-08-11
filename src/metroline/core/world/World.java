package metroline.core.world;

import metroline.objects.gameobjects.GameObject;
import metroline.core.time.GameTime;
import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.gameobjects.Label;
import metroline.objects.gameobjects.PathPoint;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.objects.enums.Direction;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.util.LngUtil;
import metroline.util.MessageUtil;
import metroline.util.MetroLogger;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class World implements Serializable {

    public GameTime gameTime;
    public static final long serialVersionUID = 1L;
    public static final String SAVE_FOLDER = "saves";
    public String SAVE_FILE;
    public Random rand = new Random();
    protected int width, height;

    protected WorldTile[][] worldGrid;
    protected GameTile[][] gameGrid;
  //  protected GameTileBig[][] bigWorldGrid;

    public java.util.List<Station> stations = new ArrayList<>();
    public java.util.List<Tunnel> tunnels = new ArrayList<>();
    public List<Label> labels = new ArrayList<>();



    public transient WorldScreen screen;

    public boolean roundStationsEnabled = false;

    public World() {
        super();
    }
    public World(WorldScreen screen, int width, int height, boolean hasOrganicPatches, boolean hasRivers, Color worldColor, String saveName) {
        this.screen = screen;
        this.width = width;
        this.height = height;
        this.gameTime = new GameTime();
        this.gameTime.start();

        this.SAVE_FILE = saveName;
        generateWorld(hasOrganicPatches, hasRivers,worldColor);
    }


     public void generateWorld(boolean hasOrganicPatches, boolean hasRivers, Color worldColor) {
        // Create world grid
        worldGrid = new WorldTile[width][height];
        gameGrid = new GameTile[width][height];
     //   bigWorldGrid = new GameTileBig[width*4][height*4];

        // Initialize with all perm=0
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                worldGrid[x][y] = new WorldTile(x, y, 0);
                worldGrid[x][y].setBaseTileColor(worldColor);
                gameGrid[x][y] = new GameTile(x, y);
            }
        }


        if (hasRivers) {
            addRivers(1);
        }

        if (hasOrganicPatches) {
            addOrganicAreas(0.5f, 8, 5, 15, 0.7f);
            addOrganicAreas(1.0f, 5, 8, 20, 0.5f);
        }

        applyGradient();
    }

    public boolean isRoundStationsEnabled() {
        return roundStationsEnabled;
    }

    public void setRoundStationsEnabled(boolean enabled) {
        this.roundStationsEnabled = enabled;
    }
    public GameTime getGameTime() {
        if (gameTime == null) {
            gameTime = new GameTime();
            gameTime.start();
        }
        return gameTime;
    }
    private void addOrganicAreas(float perm, int count, int minSize, int maxSize, float irregularity) {
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            // Случайный центр и размер пятна
            int centerX = rand.nextInt(width);
            int centerY = rand.nextInt(height);
            int size = minSize + rand.nextInt(maxSize - minSize + 1);

            // Основной радиус пятна
            float baseRadius = size / 2.0f;

            // Генерируем шум Перлина для органической формы
            float[][] noise = generateOrganicNoise(size * 2, size * 2, irregularity);

            // Определяем границы для оптимизации
            int minX = Math.max(0, centerX - size);
            int maxX = Math.min(width, centerX + size);
            int minY = Math.max(0, centerY - size);
            int maxY = Math.min(height, centerY + size);

            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    // Относительные координаты внутри пятна
                    float relX = (x - centerX) / baseRadius;
                    float relY = (y - centerY) / baseRadius;

                    // Расстояние от центра (нормализованное)
                    float dist = (float)Math.sqrt(relX * relX + relY * relY);

                    if (dist <= 1.0f) {
                        // Координаты в шумовом поле
                        int noiseX = (int)((relX + 1) * (size - 1));
                        int noiseY = (int)((relY + 1) * (size - 1));

                        // Получаем значение шума для этой точки
                        float noiseValue = noise[Math.max(0, Math.min(noise.length - 1, noiseX))]
                                [Math.max(0, Math.min(noise[0].length - 1, noiseY))];

                        // Фактор формы с учетом шума
                        float shapeFactor = 1.0f - dist + noiseValue * irregularity;

                        if (shapeFactor > 0.5f) { // Порог для создания плавных краев
                            // Плавное уменьшение к краям
                            float edgeFactor = Math.min(1.0f, shapeFactor * 2.0f);
                            worldGrid[x][y].setPerm(perm * edgeFactor);
                        }
                    }
                }
            }
        }
    }

    public boolean isLabelPositionValid(int labelX, int labelY, Station station) {
        // Проверяем, что позиция находится в пределах 2 клеток от станции
        int dx = Math.abs(labelX - station.getX());
        int dy = Math.abs(labelY - station.getY());

        return dx <= 2 && dy <= 2 && (dx + dy) > 0 && // Исключаем позицию станции
                getStationAt(labelX, labelY) == null; // И клетка свободна от станций
    }
   // public GameTileBig[][] getBigWorldGrid() { return bigWorldGrid; }
    public Label getLabelForStation(Station station) {
        for (Label label : labels) {
            if (label.getParentStation() == station) {
                return label;
            }
        }
        return null;
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
                    getStationAt(nx, ny) == null && getLabelAt(nx, ny) == null) {

                // Проверяем, что текст не будет перекрывать станцию
                if (isTextPositionGood(dir, name)) {
                    return new PathPoint(nx, ny);
                }
            }
        }

        // Если не нашли подходящую позицию, ищем в расширенном радиусе
//        for (int dy = -2; dy <= 2; dy++) {
//            for (int dx = -2; dx <= 2; dx++) {
//                if (Math.abs(dx) + Math.abs(dy) <= 1) continue; // Пропускаем ортогональные (уже проверили)
//
//                int nx = x + dx;
//                int ny = y + dy;
//
//                if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
//                        getStationAt(nx, ny) == null && getLabelAt(nx, ny) == null) {
//                    return new PathPoint(nx, ny);
//                }
//            }
//        }

        return null;
    }

    private boolean isTextPositionGood(Direction dir, String name) {
        // Для коротких названий любая позиция подходит
        if (name.length() <= 6) return true;

        // Для длинных названий предпочитаем право и низ
        return dir == Direction.EAST || dir == Direction.SOUTH ||
                dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST;
    }

    /**
     * Gets all stations
     * @return List of stations
     */
    public List<Station> getStations() { return stations; }

    /**
     * Gets all tunnels
     * @return List of tunnels
     */
    public List<Tunnel> getTunnels() { return tunnels; }

    /**
     * Gets all labels
     * @return List of labels
     */
    public List<Label> getLabels() { return labels; }

    /**
     * Adds a label to the world
     * @param label Station to add
     */
    public void addLabel(Label label) {
        labels.add(label);
        gameGrid[label.getX()][label.getY()].setContent(label);
    }
    public void removeLabel(Label label) {
        labels.remove(label);
        gameGrid[label.getX()][label.getY()].setContent(null);
    }
    /**
     * Gets the label at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return label or null if none exists
     */
    public Label getLabelAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        GameObject obj = gameGrid[x][y].getContent();
        return obj instanceof Label ? (Label)obj : null;
    }

    public List<Label> getLabelsForStation(Station station) {
        // Возвращает метки для конкретной станции
        List<Label> stationLabels = new ArrayList<>();
        for (Label label : getLabels()) {
            if (label.getParentStation().equals(station)) {
                stationLabels.add(label);
            }
        }
        return stationLabels;
    }
    /******************
     * STATIONS
     ******************/

    /**
     * Adds a station to the world
     * @param station Station to add
     */
    public void addStation(Station station) {
        if (gameGrid[station.getX()][station.getY()].getContent() != null) {
            return;
        }
        stations.add(station);
        gameGrid[station.getX()][station.getY()].setContent(station);

        // Передаем имя станции для выбора оптимальной позиции
        PathPoint labelPos = findFreePositionNear(station.getX(), station.getY(), station.getName());
        if (labelPos != null) {
            Label label = new Label(this, labelPos.x, labelPos.y, station.getName(), station);
            addLabel(label);
        }
    }

    /**
     * Removes a station from the world
     * @param station Station to remove
     */
    public void removeStation(Station station) {

        Label label = getLabelForStation(station);
        if (label != null) {
            removeLabel(label);
        }
        for (Station connectedStation : new ArrayList<>(station.getConnections().values())) {
            station.disconnect(connectedStation);
        }
        stations.remove(station);
        // Удаляем метку станции
        gameGrid[station.getX()][station.getY()].setContent(null);

        // Remove any tunnels connected to this station
        tunnels.removeIf(t -> t.getStart() == station || t.getEnd() == station);
    }
    /**
     * Gets the station at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return Station or null if none exists
     */
    public Station getStationAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        GameObject obj = gameGrid[x][y].getContent();
        return obj instanceof Station ? (Station)obj : null;
    }



    /******************
     * TUNNELS
     ******************/
    /**
     * Adds a tunnel between two stations
     * @param tunnel Tunnel to add
     * @return True if tunnel was added successfully
     */

    public void addTunnel(Tunnel tunnel) {
        // Получаем координаты станций
        int start_x = tunnel.getStart().getX();
        int start_y = tunnel.getStart().getY();
        int end_x = tunnel.getEnd().getX();
        int end_y = tunnel.getEnd().getY();

        Station actualStart = getStationAt(start_x, start_y);
        Station actualEnd = getStationAt(end_x, end_y);



        // Проверяем, существует ли уже такой туннель
        for (Tunnel existingTunnel : tunnels) {
            if ((existingTunnel.getStart() == actualStart && existingTunnel.getEnd() == actualEnd) ||
                    (existingTunnel.getStart() == actualEnd && existingTunnel.getEnd() == actualStart)) {

                return;
            }
        }

        // Создаем новый туннель с найденными станциями
        Tunnel newTunnel = new Tunnel(this, actualStart, actualEnd, tunnel.getType());

        // Подключаем станции друг к другу
        actualStart.connectStation(actualEnd);

        // Добавляем туннель в список
        tunnels.add(newTunnel);

    }
//    public void addTunnel(Tunnel tunnel) {
//
//        if (tunnel.getStart().connectStation(tunnel.getEnd())) {
//            tunnels.add(tunnel);
//        }
//
//    }
    /**
     * Gets the any game object at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return GameObject or null if none exists
     */
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

        // Если ничего не найдено
        return null;
    }
    /**
     * Gets the tunnel at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return Tunnel or null if none exists
     */
    public Tunnel getTunnelAt(int x, int y) {
        for (Tunnel tunnel : tunnels) {
            for (PathPoint p : tunnel.getPath()) {
                if (p.getX() == x && p.getY() == y) {
                    return tunnel;
                }
            }
        }
        return null;
    }
    /**
     * Removes a tunnel from the world
     * @param tunnel Tunnel to remove
     */
    public void removeTunnel(Tunnel tunnel) {
        tunnels.remove(tunnel);
        tunnel.getStart().disconnect(tunnel.getEnd());
        tunnel.getEnd().disconnect(tunnel.getStart());
    }
    // Генерация органического шума для формы пятен
    private float[][] generateOrganicNoise(int width, int height, float irregularity) {
        Random rand = new Random();
        float[][] noise = new float[width][height];

        // Заполняем шумом
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Используем простой шум для демонстрации
                noise[x][y] = rand.nextFloat() * irregularity;
            }
        }

        // Применяем размытие для сглаживания
        for (int i = 0; i < 2; i++) {
            noise = applyBlur(noise);
        }

        return noise;
    }

    // Простое размытие
    private float[][] applyBlur(float[][] input) {
        int width = input.length;
        int height = input[0].length;
        float[][] output = new float[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                int count = 0;

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;

                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            sum += input[nx][ny];
                            count++;
                        }
                    }
                }

                output[x][y] = sum / count;
            }
        }

        return output;
    }
    private void addRivers(int riverCount) {
        Random rand = new Random();

        for (int i = 0; i < riverCount; i++) {

            WorldEdge startEdge = WorldEdge.values()[rand.nextInt(WorldEdge.values().length)];
            WorldEdge endEdge;
            do {
                endEdge = WorldEdge.values()[rand.nextInt(WorldEdge.values().length)];
            } while (endEdge == startEdge); // Убедимся, что река не начинается и не заканчивается на одной стороне

            int[] startPos = getRandomEdgePosition(startEdge);
            int[] endPos = getRandomEdgePosition(endEdge);

            // Генерируем путь реки
            generateRiverPath(startPos[0], startPos[1], endPos[0], endPos[1]);
        }
    }



    private int[] getRandomEdgePosition(WorldEdge edge) {

        switch (edge) {
            case TOP:
                return new int[]{rand.nextInt(width), 0};
            case RIGHT:
                return new int[]{width - 1, rand.nextInt(height)};
            case BOTTOM:
                return new int[]{rand.nextInt(width), height - 1};
            case LEFT:
                return new int[]{0, rand.nextInt(height)};
            default:
                return new int[]{0, 0};
        }
    }

    private void generateRiverPath(int startX, int startY, int endX, int endY) {
        int currentX = startX;
        int currentY = startY;
        Random rand = new Random();
        while (true) {
            setRiverTile(currentX, currentY);
            if (Math.abs(currentX - endX) <= 1 && Math.abs(currentY - endY) <= 1) {
                setRiverTile(endX, endY);
                break;
            }
            int dx = Integer.compare(endX, currentX);
            int dy = Integer.compare(endY, currentY);

            if (rand.nextBoolean()) {
                if (rand.nextBoolean() && currentX + dx >= 0 && currentX + dx < width) {
                    currentX += dx;
                } else if (currentY + dy >= 0 && currentY + dy < height) {
                    currentY += dy;
                }
            } else {
                if (currentX + dx >= 0 && currentX + dx < width &&
                        currentY + dy >= 0 && currentY + dy < height) {
                    currentX += dx;
                    currentY += dy;
                }
            }
        }
    }

    private void setRiverTile(int x, int y) {
        worldGrid[x][y].setPerm(0.4f);
        for (int ny = Math.max(0, y - 1); ny <= Math.min(height - 1, y + 1); ny++) {
            for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++) {
                if (nx != x || ny != y) {
                    worldGrid[nx][ny].setPerm(0.4f);
                }
            }
        }
    }
    /**
     * Applies gradient smoothing around permission boundaries
     */
    private void applyGradient() {
        // Make a copy for reference
        float[][] originalPerm = new float[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                originalPerm[x][y] = worldGrid[x][y].getPerm();
            }
        }

        // Apply smoothing
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (originalPerm[x][y] == 0 || originalPerm[x][y] == 1) {
                    // Only smooth at boundaries
                    boolean hasNeighbor = false;
                    for (int ny = Math.max(0, y-1); ny < Math.min(height, y+2); ny++) {
                        for (int nx = Math.max(0, x-1); nx < Math.min(width, x+2); nx++) {
                            if (originalPerm[nx][ny] != originalPerm[x][y]) {
                                hasNeighbor = true;
                                break;
                            }
                        }
                        if (hasNeighbor) break;
                    }

                    if (hasNeighbor) {
                        // Calculate average of neighbors
                        float sum = 0;
                        int count = 0;
                        for (int ny = Math.max(0, y-1); ny < Math.min(height, y+2); ny++) {
                            for (int nx = Math.max(0, x-1); nx < Math.min(width, x+2); nx++) {
                                if (nx != x || ny != y) {
                                    sum += originalPerm[nx][ny];
                                    count++;
                                }
                            }
                        }
                        worldGrid[x][y].setPerm(sum / count);
                    }
                }
            }
        }
    }

    public Color getWorldColorAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return worldGrid[x][y].getCurrentColor();
    }
    public WorldTile getWorldTile(int x, int y) {
        return worldGrid[x][y];
    }
    public GameTile getGameTile(int x, int y) {
        return gameGrid[x][y];
    }
    /**
     * Gets the world grid
     * @return 2D array of world tiles
     */
    public WorldTile[][] getWorldGrid() { return worldGrid; }

    /**
     * Gets the game grid
     * @return 2D array of game tiles
     */
    public GameTile[][] getGameGrid() { return gameGrid; }

    public WorldScreen getWorldScreen() {
        if (this instanceof GameWorld) {
            return WorldGameScreen.getInstance();
//        } else if (this instanceof SandboxWorld) {
//            return WorldSandboxScreen.getInstance();
       }
        return null;
    }
    public World getWorld() {
        return this;
    }
    /**
     * Gets the width of the world
     * @return Width in tiles
     */
    public int getWidth() { return width; }
    public void setWidth(int width) {
        this.width = width;
    }
    /**
     * Gets the height of the world
     * @return Height in tiles
     */
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }

    /**************************
     * SAVE AND LOAD SECTIONS
     */
    public void saveWorld() {
        File saveDir = new File(GameWorld.SAVE_FOLDER);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }
        File saveFile = new File(GameWorld.SAVE_FOLDER + File.separator + SAVE_FILE);

        // Создаем временный объект только с нужными данными
        World saveData = new World();
        saveData.width = this.width;
        saveData.height = this.height;
        saveData.worldGrid = this.worldGrid;
        saveData.gameGrid = this.gameGrid;
        saveData.stations = this.stations;
        saveData.tunnels = this.tunnels;
        saveData.labels = this.labels;
        saveData.gameTime = this.gameTime;
        saveData.roundStationsEnabled = this.roundStationsEnabled;

        saveData.SAVE_FILE = this.SAVE_FILE;

        try {
            // Проверка сериализуемости перед сохранением
            new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(this);
        } catch (IOException e) {
            MetroLogger.logError("World is not serializable", e);
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile))) {
            oos.writeObject(saveData);

            MetroLogger.logInfo("World successfully saved");
            MessageUtil.showTimedMessage(LngUtil.translatable("world.saved"), false, 2000);
        } catch (IOException ex) {
            MetroLogger.logError("Failed to save world", ex);
            MessageUtil.showTimedMessage(LngUtil.translatable("world.not_saved") + ex.getMessage(), true, 2000);
        }
    }

    public boolean loadWorld() {
        File saveFile = new File(GameWorld.SAVE_FOLDER + File.separator + SAVE_FILE);

        if (!saveFile.exists()) {
            return false;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            World loaded = (World) ois.readObject();

            this.width = loaded.width;
            this.height = loaded.height;
            this.worldGrid = loaded.worldGrid;
            this.gameGrid = loaded.gameGrid;
            this.stations = loaded.stations;
            this.tunnels = loaded.tunnels;
            this.labels = loaded.labels;
            this.gameTime = loaded.gameTime;
            this.roundStationsEnabled = loaded.roundStationsEnabled;

           this.SAVE_FILE = loaded.SAVE_FILE;


            if (loaded.getGameTime() != null) {
                loaded.getGameTime().start();
            } else {
                loaded.gameTime = new GameTime();
                loaded.gameTime.start();
            }

            if (this.screen != null) {
                this.screen.reinitializeControllers();
            }
            MetroLogger.logInfo("World successfully loaded");
            MessageUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            MetroLogger.logError("Failed to load world", ex);
            MessageUtil. showTimedMessage(LngUtil.translatable("world.not_loaded") + ex.getMessage(), true, 2000);
        }
        return false;
    }
}
