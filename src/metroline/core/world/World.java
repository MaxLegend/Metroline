package metroline.core.world;

import metroline.objects.gameobjects.*;
import metroline.core.time.GameTime;
import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.enums.Direction;
import metroline.objects.gameobjects.Label;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.util.*;
import metroline.util.localizate.LngUtil;
import metroline.util.serialize.MetroSerializer;
import metroline.util.ui.UserInterfaceUtil;

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
    protected short width, height;

    //  public WorldTile[][] worldGrid;
    public WorldTile[] worldGrid;
    public GameTile[] gameGrid;
    public GameTile[] gameplayGrid;

    public java.util.List<Station> stations = new ArrayList<>();
    public java.util.List<Tunnel> tunnels = new ArrayList<>();
    public List<Label> labels = new ArrayList<>();

    public transient WorldScreen screen;
    public boolean roundStationsEnabled = false;


    public World() {
        super();
    }

    public World(WorldScreen screen, short width, short height,boolean hasPassengerCount, boolean hasAbilityPay, boolean hasLandscape, boolean hasRivers, int worldColor, String saveName) {
        this.screen = screen;
        this.width = width;
        this.height = height;
        this.gameTime = new GameTime();
        this.gameTime.start();
        this.SAVE_FILE = saveName;
 //
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

    public void initWorldGrid() {
        worldGrid = new WorldTile[width * height];
        for (short y = 0; y < height; y++) {
            for (short x = 0; x < width; x++) {
                int index = y * width + x;
                worldGrid[index] = new WorldTile(x, y);
            }
        }
    }
    public void initGameGrid() {
        gameGrid = new GameTile[width * height];
        for (short y = 0; y < height; y++) {
            for (short x = 0; x < width; x++) {
                int index = y * width + x;
                gameGrid[index] = new GameTile(x, y);
            }
        }
    }
    public void initGameplayGrid() {
        gameplayGrid = new GameTile[width * height];
        for (short y = 0; y < height; y++) {
            for (short x = 0; x < width; x++) {
                int index = y * width + x;
                gameplayGrid[index] = new GameTile(x, y);
            }
        }
    }
    public Label getLabelForGameObject(GameObject station) {
        for (Label label : labels) {
            if (label.getParentGameObject() == station) {
                return label;
            }
        }
        return null;
    }

    public Label getLabelForStation(Station station) {
        for (Label label : labels) {
            if (label.getParentGameObject() == station) {
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
                    getStationAt(nx, ny) == null && getLabelAt(nx, ny) == null&& getLabelAt(nx, ny) == null) {

                // Проверяем, что текст не будет перекрывать станцию
                if (isTextPositionGood(dir, name)) {
                    return new PathPoint(nx, ny);
                }
            }
        }

        return null;
    }

    boolean isTextPositionGood(Direction dir, String name) {
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
        getGameTile(label.getX(), label.getY()).setContent(label);

    }
    public void removeLabel(Label label) {
        labels.remove(label);
        getGameTile(label.getX(), label.getY()).setContent(null);

    }
    /**
     * Gets the label at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return label or null if none exists
     */
    public Label getLabelAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        GameObject obj = getGameTile(x, y).getContent();
        return obj instanceof Label ? (Label)obj : null;
    }

    public List<Label> getLabelsForStation(Station station) {
        // Возвращает метки для конкретной станции
        List<Label> stationLabels = new ArrayList<>();
        for (Label label : getLabels()) {
            if (label.getParentGameObject().equals(station)) {
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
        if (getGameTile(station.getX(), station.getY()).getContent() != null) {
            return;
        }
        stations.add(station);
        getGameTile(station.getX(), station.getY()).setContent(station);

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
        getGameTile(station.getX(), station.getY()).setContent(null);

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
        GameObject obj = getGameTile(x, y).getContent();
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


    private WorldEdge getOppositeEdge(WorldEdge edge) {
        switch (edge) {
            case TOP: return WorldEdge.BOTTOM;
            case BOTTOM: return WorldEdge.TOP;
            case LEFT: return WorldEdge.RIGHT;
            case RIGHT: return WorldEdge.LEFT;
            default: return WorldEdge.TOP;
        }
    }

    // Замените старый метод addRivers на этот:
    void addRivers(int riverCount) {
        for (int i = 0; i < riverCount; i++) {
            WorldEdge startEdge = WorldEdge.values()[rand.nextInt(WorldEdge.values().length)];
            WorldEdge endEdge = getOppositeEdge(startEdge);

            // Получаем начальную и конечную точки в мировых координатах
            PathPoint startPos = getRandomEdgePathPoint(startEdge);
            PathPoint endPos = getRandomEdgePathPoint(endEdge);

            generateMeanderingRiver(startPos, endPos, rand.nextFloat() * 0.3f + 0.1f);
        }
    }

    private PathPoint getRandomEdgePathPoint(WorldEdge edge) {
        int x, y;
        int edgeOffset = 3; // Отступ от края карты

        switch (edge) {
            case TOP:
                x = rand.nextInt(width - edgeOffset*2) + edgeOffset;
                y = 0;
                break;
            case RIGHT:
                x = width - 1;
                y = rand.nextInt(height - edgeOffset*2) + edgeOffset;
                break;
            case BOTTOM:
                x = rand.nextInt(width - edgeOffset*2) + edgeOffset;
                y = height - 1;
                break;
            case LEFT:
                x = 0;
                y = rand.nextInt(height - edgeOffset*2) + edgeOffset;
                break;
            default:
                x = 0;
                y = 0;
        }

        // Создаем PathPoint с мировыми координатами
        return new PathPoint(x, y);
    }

    private void generateMeanderingRiver(PathPoint start, PathPoint end, float meanderAmount) {
        List<PathPoint> riverPath = new ArrayList<>();
        riverPath.add(start);

        int currentX = start.getX();
        int currentY = start.getY();
        Random rand = new Random();

        // Основное направление к конечной точке
        int dx = Integer.compare(end.getX(), currentX);
        int dy = Integer.compare(end.getY(), currentY);

        // Перпендикулярное направление для изгибов
        int perpX = -dy;
        int perpY = dx;

        while (!(currentX == end.getX() && currentY == end.getY())) {
            // Основное движение к цели (70% вероятность)
            if (rand.nextFloat() < 0.7) {
                currentX = clamp(currentX + dx, 0, width-1);
                currentY = clamp(currentY + dy, 0, height-1);
            }

            // Добавляем изгибы (40% вероятность)
            if (rand.nextFloat() < 0.4) {
                int offset = rand.nextBoolean() ? 1 : -1;
                currentX = clamp(currentX + perpX * offset, 0, width-1);
                currentY = clamp(currentY + perpY * offset, 0, height-1);
            }

            // Добавляем точку только если она изменилась
            PathPoint last = riverPath.get(riverPath.size()-1);
            if (currentX != last.getX() || currentY != last.getY()) {
                riverPath.add(new PathPoint(currentX, currentY));
            }

            // Защита от зацикливания
            if (riverPath.size() > width * height) break;
        }

        // Сглаживаем путь реки
        riverPath = smoothRiverPath(riverPath);

        // Устанавливаем тайлы реки
        for (PathPoint point : riverPath) {
            setRiverTile(point.getX(), point.getY(), 2 + rand.nextInt(2)); // Ширина 2-3 тайла
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private List<PathPoint> smoothRiverPath(List<PathPoint> path) {
        if (path.size() < 3) return path;

        List<PathPoint> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        for (int i = 1; i < path.size() - 1; i++) {
            PathPoint prev = path.get(i-1);
            PathPoint curr = path.get(i);
            PathPoint next = path.get(i+1);

            // Усредняем только угловые точки
            if (prev.getX() != next.getX() && prev.getY() != next.getY()) {
                int avgX = (prev.getX() + curr.getX() + next.getX()) / 3;
                int avgY = (prev.getY() + curr.getY() + next.getY()) / 3;
                smoothed.add(new PathPoint(avgX, avgY));
            } else {
                smoothed.add(curr);
            }
        }

        smoothed.add(path.get(path.size()-1));
        return smoothed;
    }




    private void setRiverTile(int x, int y, int radius) {
        getWorldTile(x,y).setPerm(0.3f);
        getWorldTile(x,y).setWater(true);
        getWorldTile(x,y).setWaterDepth(1.0f); // Центр - максимальная глубина

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                if (dist <= radius) {
                    int nx = x + dx;
                    int ny = y + dy;

                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        float perm = 0.3f + (dist/radius) * 0.4f;
                        getWorldTile(nx,ny).setPerm(perm);

                        // Устанавливаем глубину (1 в центре, 0 на краях)
                        float depth = 1.0f - (dist / radius);
                        if (dist < radius * 0.7f) {
                            getWorldTile(nx,ny).setWater(true);
                            getWorldTile(nx,ny).setWaterDepth(depth);
                        }
                    }
                }
            }
        }
    }




    private boolean canMove(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Applies gradient smoothing around permission boundaries
     */
    void applyGradient() {
        // Make a copy for reference
        float[][] originalPerm = new float[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                originalPerm[x][y] = getWorldTile(x,y).getPerm();
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
                        getWorldTile(x,y).setPerm(sum / count);
                    }
                }
            }
        }
    }

    public Color getWorldColorAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return getWorldTile(x,y).getCurrentColor();
    }

    private int index(int x, int y) {
        return y * width + x;
    }

public WorldTile getWorldTile(int x, int y) {
    //    System.out.println("getWorldTile: " + x + ", " + y);
    if (x < 0 || x >= width || y < 0 || y >= height) return null;
    return worldGrid[index(x, y)];
}
    public void setWorldTile(int x, int y, WorldTile tile) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        worldGrid[index(x, y)] = tile;

    }

    public GameTile getGameTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return gameGrid[index(x, y)];
    }
    public void setGameTile(int x, int y, GameTile tile) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        gameGrid[index(x, y)] = tile;
    }
    public GameTile getGameplayTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return gameplayGrid[index(x, y)];
    }
    public void setGameplayTile(int x, int y, GameTile tile) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        gameplayGrid[index(x, y)] = tile;
    }
    /**
     * Gets the world grid
     * @return 2D array of world tiles
     */
//    public WorldTile[][] getWorldGrid() { return worldGrid; }
    public WorldTile[] getWorldGrid() {
        return worldGrid;
    }
    /**
     * Gets the game grid
     * @return 2D array of game tiles
     */
    public GameTile[] getGameGrid() { return gameGrid; }

    /**
     * Gets the gameplay grid
     * @return 2D array of game tiles
     */
    public GameTile[] getGameplayGrid() { return gameplayGrid; }
    public WorldScreen getWorldScreen() {
        if (this instanceof GameWorld) {
            return GameWorldScreen.getInstance();
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
    public void setWidth(short width) {
        this.width = width;
    }
    /**
     * Gets the height of the world
     * @return Height in tiles
     */
    public int getHeight() {
        return height;
    }
    public void setHeight(short height) {
        this.height = height;
    }

    /**************************
     * SAVE AND LOAD SECTIONS
     */
    public void saveWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            if (this instanceof GameWorld) {
                serializer.saveWorld((GameWorld) this, SAVE_FILE);
            } else if (this instanceof SandboxWorld) {
                // Если у SandboxWorld свой формат сохранения
           //     serializer.saveWorld((GameWorld) this, SAVE_FILE); // или адаптируйте под SandboxWorld
            }

            MetroLogger.logInfo("World successfully saved");
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.saved"), false, 2000);
        } catch (IOException ex) {
            MetroLogger.logError("Failed to save world", ex);
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.not_saved") + ex.getMessage(), true, 2000);
        }
    }

    public boolean loadWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();

            if (this instanceof GameWorld) {
                GameWorld loaded = serializer.loadWorld(SAVE_FILE);
                copyWorldData(loaded);
            } else if (this instanceof SandboxWorld) {
//                GameWorld loaded = serializer.loadWorld(SAVE_FILE);
//                copyWorldData(loaded);
            }

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
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            MetroLogger.logError("Failed to load world", ex);
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.not_loaded") + ex.getMessage(), true, 2000);
        }
        return false;
    }
    private void copyWorldData(World source) {
        this.width = source.width;
        this.height = source.height;
        this.worldGrid = source.worldGrid;
        this.gameGrid = source.gameGrid;
        this.stations = source.stations;
        this.tunnels = source.tunnels;
        this.labels = source.labels;
        this.gameTime = source.gameTime;
        this.roundStationsEnabled = source.roundStationsEnabled;
        this.SAVE_FILE = source.SAVE_FILE;
    }

}
