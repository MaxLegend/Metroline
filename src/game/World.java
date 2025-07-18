package game;

import game.tiles.GameTile;
import game.tiles.WorldTile;

import java.util.Random;

public class World {
    private WorldTile[][] worldLayer;
    private GameTile[][] gameLayer;
    private int width, height;
    private int tileSize;

    public World(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;

        generateWorld();
        generateGameLayer();
    }

    /**
     * Генерация базового слоя мира с перлиновым шумом
     */
    private void generateWorld() {
        worldLayer = new WorldTile[width][height];
        Random random = new Random();

        // Заполняем базовый слой
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                worldLayer[x][y] = new WorldTile(x, y, tileSize);
            }
        }

        // Добавляем области с разной сложностью строительства
        addAreas(0.5f, 10, 15, random);
        addAreas(1f, 5, 8, random);

        // Создаем градиентные переходы
        smoothPermValues();
    }

    /**
     * Добавление областей с определенным значением perm
     * @param value - значение perm
     * @param numAreas - количество областей
     * @param maxSize - максимальный размер области
     * @param random - генератор случайных чисел
     */
    private void addAreas(float value, int numAreas, int maxSize, Random random) {
        for (int i = 0; i < numAreas; i++) {
            int centerX = random.nextInt(width);
            int centerY = random.nextInt(height);
            int size = random.nextInt(maxSize) + 1;

            for (int x = Math.max(0, centerX - size); x < Math.min(width, centerX + size); x++) {
                for (int y = Math.max(0, centerY - size); y < Math.min(height, centerY + size); y++) {
                    double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                    if (distance <= size) {
                        worldLayer[x][y].setPerm(value);
                    }
                }
            }
        }
    }

    /**
     * Сглаживание значений perm для создания градиента
     */
    private void smoothPermValues() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (worldLayer[x][y].getPerm() > 0) {
                    applyGradient(x, y);
                }
            }
        }
    }

    /**
     * Применение градиента вокруг точки
     */
    private void applyGradient(int x, int y) {
        float centerValue = worldLayer[x][y].getPerm();
        int radius = 3; // Радиус влияния градиента

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    float distance = (float) Math.sqrt(dx*dx + dy*dy);
                    if (distance > 0) {
                        float newValue = centerValue - distance * 0.1f;
                        if (newValue > worldLayer[nx][ny].getPerm()) {
                            worldLayer[nx][ny].setPerm(newValue);
                        }
                    }
                }
            }
        }
    }

    /**
     * Генерация игрового слоя
     */
    private void generateGameLayer() {
        gameLayer = new GameTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                gameLayer[x][y] = new GameTile(x, y, tileSize);
            }
        }
    }

    // Геттеры для слоев
    public WorldTile[][] getWorldLayer() { return worldLayer; }
    public GameTile[][] getGameLayer() { return gameLayer; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTileSize() { return tileSize; }
}
