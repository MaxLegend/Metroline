package game.core.world;

import game.core.GameObject;
import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.tiles.GameTile;

import game.tiles.GameTileBig;
import game.tiles.WorldTile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * World class containing game logic and state
 */
public class World {
    private WorldTile[][] worldGrid;
    private GameTile[][] gameGrid;
    private GameTileBig[][] bigWorldGrid;
    Random rand = new Random();
    private List<Station> stations = new ArrayList<>();
    private List<Tunnel> tunnels = new ArrayList<>();

    private int width, height;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        generateWorld();
    }

    /**
     * Generates the world with terrain permissions
     */
    private void generateWorld() {
        // Create world grid
        worldGrid = new WorldTile[width][height];
        gameGrid = new GameTile[width][height];
        bigWorldGrid = new GameTileBig[width*4][height*4];

        // Initialize with all perm=0
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                worldGrid[x][y] = new WorldTile(x, y, 0);
                gameGrid[x][y] = new GameTile(x, y);
            }
        }
        for (int y = 0; y < height*4; y++) {
            for (int x = 0; x < width*4; x++) {
                bigWorldGrid[x][y] = new GameTileBig(x, y);
            }
        }
        addRivers(1);
        // Add some perm=0.5 areas
        addOrganicAreas(0.5f, 8, 5, 15, 0.7f);

        // Add some perm=1 areas (smaller)
        addOrganicAreas(1.0f, 5, 8, 20, 0.5f);


        // Apply gradient smoothing
            applyGradient();

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
            // Выбираем случайные точки на краях карты
            Edge startEdge = Edge.values()[rand.nextInt(Edge.values().length)];
            Edge endEdge;
            do {
                endEdge = Edge.values()[rand.nextInt(Edge.values().length)];
            } while (endEdge == startEdge); // Убедимся, что река не начинается и не заканчивается на одной стороне

            int[] startPos = getRandomEdgePosition(startEdge);
            int[] endPos = getRandomEdgePosition(endEdge);

            // Генерируем путь реки
            generateRiverPath(startPos[0], startPos[1], endPos[0], endPos[1]);
        }
    }

    private enum Edge {
        TOP, RIGHT, BOTTOM, LEFT
    }

    private int[] getRandomEdgePosition(Edge edge) {

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
            // Устанавливаем значение perm для текущей позиции и соседей (толщина 3 клетки)
            setRiverTile(currentX, currentY);

            // Проверяем, достигли ли мы конечной точки
            if (Math.abs(currentX - endX) <= 1 && Math.abs(currentY - endY) <= 1) {
                setRiverTile(endX, endY);
                break;
            }

            // Определяем направление движения
            int dx = Integer.compare(endX, currentX);
            int dy = Integer.compare(endY, currentY);

            // Случайно решаем, двигаться по оси X или Y (или по диагонали)
            if (rand.nextBoolean()) {
                // Движение по одной оси
                if (rand.nextBoolean() && currentX + dx >= 0 && currentX + dx < width) {
                    currentX += dx;
                } else if (currentY + dy >= 0 && currentY + dy < height) {
                    currentY += dy;
                }
            } else {
                // Диагональное движение (если возможно)
                if (currentX + dx >= 0 && currentX + dx < width &&
                        currentY + dy >= 0 && currentY + dy < height) {
                    currentX += dx;
                    currentY += dy;
                }
            }
        }
    }

    private void setRiverTile(int x, int y) {
        // Устанавливаем значение perm для центральной клетки
        worldGrid[x][y].setPerm(0.4f);

        // Устанавливаем значение perm для соседних клеток (толщина реки 3 клетки)
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



    public GameTileBig[][] getBigWorldGrid() { return bigWorldGrid; }


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
     * Adds a station to the world
     * @param station Station to add
     */
    public void addStation(Station station) {
        stations.add(station);

        gameGrid[station.getX()][station.getY()].setContent(station);
    }

    /**
     * Removes a station from the world
     * @param station Station to remove
     */
    public void removeStation(Station station) {
        for (Station connectedStation : new ArrayList<>(station.getConnections().values())) {
            station.disconnect(connectedStation);
        }
        stations.remove(station);
        gameGrid[station.getX()][station.getY()].setContent(null);

        // Remove any tunnels connected to this station
        tunnels.removeIf(t -> t.getStart() == station || t.getEnd() == station);
    }

    /**
     * Adds a tunnel between two stations
     * @param tunnel Tunnel to add
     * @return True if tunnel was added successfully
     */
    public boolean addTunnel(Tunnel tunnel) {
        if (tunnel.getStart().connect(tunnel.getEnd())) {
            tunnels.add(tunnel);
            return true;
        }
        return false;
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
     * Gets the width of the world
     * @return Width in tiles
     */
    public int getWidth() { return width; }

    /**
     * Gets the height of the world
     * @return Height in tiles
     */
    public int getHeight() { return height; }
}

