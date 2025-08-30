package metroline.objects.gameobjects;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.core.world.cities.CityManager;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.GameplayUnitsType;
import metroline.util.MetroLogger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

import static metroline.core.world.cities.CityDistrict.EXPANSION_CHANCES;

public class GameplayUnits extends GameObject {

    private boolean hasExpanded = false;
    private int expansionCount = 0;
    private static final int MAX_EXPANSIONS = 3; // Максимум 3 расширения на здание

    private final GameplayUnitsType type;
    private static final Map<GameplayUnitsType, BufferedImage> icons = new HashMap<>();
    private final long creationTime;
    private int condition; // Состояние здания (0-100)
    private boolean isAbandoned;
    static {
        try {
            icons.put(GameplayUnitsType.PORT, loadIcon("port.png"));
            icons.put(GameplayUnitsType.AIRPORT, loadIcon("airport.png"));
            icons.put(GameplayUnitsType.MUSEUM, loadIcon("museum.png"));
            icons.put(GameplayUnitsType.HOUSE_CULTURE, loadIcon("house_culture.png"));
            icons.put(GameplayUnitsType.CHURCH, loadIcon("church.png"));
            icons.put(GameplayUnitsType.CITYHALL, loadIcon("cityhall.png"));
            icons.put(GameplayUnitsType.SHOP, loadIcon("shop.png"));
            icons.put(GameplayUnitsType.FACTORY, loadIcon("factory.png"));
            icons.put(GameplayUnitsType.FACTORY2, loadIcon("factory2.png"));
            icons.put(GameplayUnitsType.FACTORY3, loadIcon("factory3.png"));
            icons.put(GameplayUnitsType.FACTORY4, loadIcon("factory4.png"));
            icons.put(GameplayUnitsType.FACTORY5, loadIcon("factory5.png"));
            icons.put(GameplayUnitsType.BIG_HOUSE, loadIcon("big_house.png"));
            icons.put(GameplayUnitsType.HOUSE, loadIcon("house.png"));
            icons.put(GameplayUnitsType.SMALL_HOUSE, loadIcon("small_house.png"));
            icons.put(GameplayUnitsType.PERSONAL_HOUSE, loadIcon("personal_house.png"));
            icons.put(GameplayUnitsType.BIG_PERSONAL_HOUSE, loadIcon("big_personal_house.png"));
        } catch (IOException e) {
            e.printStackTrace();
            // Запасной вариант - создаем простые иконки
            createFallbackIcons();
        }
    }

    private static final Random random = new Random();

    // Шансы расширения для разных типов зданий
    private static final Map<GameplayUnitsType, Float> EXPANSION_CHANCES = new HashMap<>();
    static {
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY, 0.12f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY2, 0.10f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY3, 0.08f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY4, 0.06f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY5, 0.05f);
        EXPANSION_CHANCES.put(GameplayUnitsType.SHOP, 0.08f);
        EXPANSION_CHANCES.put(GameplayUnitsType.HOUSE, 0.04f);
        EXPANSION_CHANCES.put(GameplayUnitsType.BIG_HOUSE, 0.05f);
        EXPANSION_CHANCES.put(GameplayUnitsType.SMALL_HOUSE, 0.03f);
        EXPANSION_CHANCES.put(GameplayUnitsType.CITYHALL, 0.15f);
        EXPANSION_CHANCES.put(GameplayUnitsType.CHURCH, 0.07f);
        EXPANSION_CHANCES.put(GameplayUnitsType.HOUSE_CULTURE, 0.06f);
        EXPANSION_CHANCES.put(GameplayUnitsType.MUSEUM, 0.05f);
    }

    // Радиусы влияния для разных типов зданий
    private static final Map<GameplayUnitsType, Integer> EXPANSION_RADIUS = new HashMap<>();
    static {
        EXPANSION_RADIUS.put(GameplayUnitsType.FACTORY, 4);
        EXPANSION_RADIUS.put(GameplayUnitsType.FACTORY2, 5);
        EXPANSION_RADIUS.put(GameplayUnitsType.FACTORY3, 5);
        EXPANSION_RADIUS.put(GameplayUnitsType.FACTORY4, 6);
        EXPANSION_RADIUS.put(GameplayUnitsType.FACTORY5, 6);
        EXPANSION_RADIUS.put(GameplayUnitsType.SHOP, 3);
        EXPANSION_RADIUS.put(GameplayUnitsType.HOUSE, 2);
        EXPANSION_RADIUS.put(GameplayUnitsType.BIG_HOUSE, 3);
        EXPANSION_RADIUS.put(GameplayUnitsType.SMALL_HOUSE, 2);
        EXPANSION_RADIUS.put(GameplayUnitsType.CITYHALL, 8);
        EXPANSION_RADIUS.put(GameplayUnitsType.CHURCH, 4);
        EXPANSION_RADIUS.put(GameplayUnitsType.HOUSE_CULTURE, 4);
        EXPANSION_RADIUS.put(GameplayUnitsType.MUSEUM, 4);
    }

    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }
    private static BufferedImage loadIcon(String path) throws IOException {

        return ImageIO.read(GameplayUnits.class.getResourceAsStream("/units_icon/" + path));
    }
    public GameplayUnits(World world, int x, int y, GameplayUnitsType type) {
        super(world, x, y);
        this.type = type;
        this.name = type.getLocalizedName();
        this.creationTime = world.getGameTime().getCurrentTimeMillis();
        this.condition = 80 + world.rand.nextInt(20); // 80-100% initially
        this.isAbandoned = false;
    }

    public void updateCondition() {
        if (!isAbandoned && getWorld().rand.nextFloat() < 0.01f) {
            condition = Math.max(0, condition - 1);

            // Увеличиваем шанс забрасывания
            if (condition < 40 && getWorld().rand.nextFloat() < 0.08f) {
                abandon();
            }

            // Шанс полного разрушения очень старых зданий
            long age = getWorld().getGameTime().getCurrentTimeMillis() - creationTime;
            if (age > 86400000 && random.nextFloat() < 0.001f) { // После 24 часов
                destroyBuilding();
            }
        }
    }
    private void abandon() {
        isAbandoned = true;
        // Можно изменить внешний вид заброшенного здания
    }
    public void tryExpand() {
        // Проверяем лимиты расширения
        if (hasExpanded && expansionCount >= MAX_EXPANSIONS) return;
        if (isAbandoned) return;
        if (condition < 60) return; // Только здания в хорошем состоянии

        CityManager cityManager = ((GameWorld)getWorld()).getCityManager();
        if (cityManager == null) return;

        // СИЛЬНО уменьшаем базовый шанс
        float expansionChance = getExpansionChance()*0.8f; // Уменьшаем в 10 раз
        if (random.nextFloat() > expansionChance) return;

        int radius = getExpansionRadius();
        Point expansionPoint = findExpansionPosition(radius);

        if (expansionPoint != null) {
            GameplayUnitsType newBuildingType = determineExpansionBuildingType();
            createNewBuilding(expansionPoint, newBuildingType);
            expansionCount++;
            hasExpanded = true;
        }
    }

    private Point findExpansionPosition(int maxRadius) {
        // Пробуем сначала ближайшие клетки (расстояние 1), затем дальние
        for (int distance = 1; distance <= maxRadius; distance++) {
            // Создаем список всех возможных позиций на этом расстоянии
            List<Point> possiblePositions = new ArrayList<>();

            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance; dy <= distance; dy++) {
                    // Пропускаем диагонали для более плотной застройки
                    if (Math.abs(dx) == Math.abs(dy) && distance > 1) continue;

                    // Пропускаем слишком далекие клетки (outside the circle)
                    if (Math.sqrt(dx*dx + dy*dy) > distance + 0.5) continue;

                    int x = this.x + dx;
                    int y = this.y + dy;

                    if (isValidExpansionPosition(x, y)) {
                        // Даем приоритет соседним клеткам (расстояние 1)
                        int priority = (distance == 1) ? 10 : 1;
                        for (int i = 0; i < priority; i++) {
                            possiblePositions.add(new Point(x, y));
                        }
                    }
                }
            }

            // Если нашли подходящие позиции на этом расстоянии, выбираем случайную
            if (!possiblePositions.isEmpty()) {
                return possiblePositions.get(random.nextInt(possiblePositions.size()));
            }
        }
        return null;
    }
    private boolean isValidExpansionPosition(int x, int y) {
        // Проверяем границы мира
        if (x < 0 || x >= getWorld().getWidth() || y < 0 || y >= getWorld().getHeight()) {
            return false;
        }

        // Проверяем, что не вода
        if (getWorld().getWorldTile(x, y).isWater()) {
            return false;
        }

        // Проверяем, что клетка свободна
        if (
                ((GameWorld)getWorld()).getGameplayUnitsAt(x, y) != null) {
            return false;
        }

        // Уменьшаем минимальное расстояние до 1 клетки для плотной застройки
        return hasEnoughSpace(x, y, 1); // Было 2, стало 1
    }

    // Уменьшаем минимальное расстояние между зданиями
    private boolean hasEnoughSpace(int x, int y, int minDistance) {
        List<GameplayUnits> allBuildings = ((GameWorld)getWorld()).getGameplayUnits();
        for (GameplayUnits building : allBuildings) {
            // Используем манхэттенское расстояние для более плотной застройки
            int manhattanDistance = Math.abs(x - building.getX()) + Math.abs(y - building.getY());
            if (manhattanDistance < minDistance) {
                return false;
            }

            // Также проверяем евклидово расстояние, но с меньшим требованием
            double euclideanDistance = Math.sqrt(Math.pow(x - building.getX(), 2) +
                    Math.pow(y - building.getY(), 2));
            if (euclideanDistance < minDistance - 0.5) {
                return false;
            }
        }
        return true;
    }
    private GameplayUnitsType determineExpansionBuildingType() {
        // Логика определения типа нового здания на основе текущего
        switch (this.type) {
            case FACTORY:
            case FACTORY2:
            case FACTORY3:
            case FACTORY4:
            case FACTORY5:
                // Рядом с фабриками - жилье или магазины
                if (random.nextFloat() < 0.7f) {
                    return getRandomResidentialType();
                } else {
                    return GameplayUnitsType.SHOP;
                }

            case HOUSE:
            case BIG_HOUSE:
            case SMALL_HOUSE:
            case PERSONAL_HOUSE:
            case BIG_PERSONAL_HOUSE:
                // Рядом с жильем - магазины или сервисы
                if (random.nextFloat() < 0.6f) {
                    return GameplayUnitsType.SHOP;
                } else {
                    return getRandomServiceType();
                }

            case SHOP:
                // Рядом с магазинами - больше магазинов или жилья
                if (random.nextFloat() < 0.5f) {
                    return GameplayUnitsType.SHOP;
                } else {
                    return getRandomResidentialType();
                }

            case CITYHALL:
                // Рядом с мэрией - важные здания
                return getRandomImportantType();

            case CHURCH:
            case HOUSE_CULTURE:
            case MUSEUM:
                // Рядом с культурными зданиями - жилье или магазины
                if (random.nextFloat() < 0.6f) {
                    return getRandomResidentialType();
                } else {
                    return GameplayUnitsType.SHOP;
                }

            default:
                return getWeightedRandomType();
        }
    }

    private GameplayUnitsType getRandomResidentialType() {
        GameplayUnitsType[] residentialTypes = {
                GameplayUnitsType.SMALL_HOUSE,
                GameplayUnitsType.HOUSE,
                GameplayUnitsType.BIG_HOUSE,
                GameplayUnitsType.PERSONAL_HOUSE,
                GameplayUnitsType.BIG_PERSONAL_HOUSE
        };
        return residentialTypes[random.nextInt(residentialTypes.length)];
    }

    private GameplayUnitsType getRandomServiceType() {
        GameplayUnitsType[] serviceTypes = {
                GameplayUnitsType.CHURCH,
                GameplayUnitsType.HOUSE_CULTURE,
                GameplayUnitsType.MUSEUM,
                GameplayUnitsType.SHOP
        };
        return serviceTypes[random.nextInt(serviceTypes.length)];
    }

    private GameplayUnitsType getRandomImportantType() {
        GameplayUnitsType[] importantTypes = {
                GameplayUnitsType.CITYHALL,
                GameplayUnitsType.CHURCH,
                GameplayUnitsType.MUSEUM,
                GameplayUnitsType.HOUSE_CULTURE
        };
        return importantTypes[random.nextInt(importantTypes.length)];
    }

    private GameplayUnitsType getWeightedRandomType() {
        // Простая взвешенная случайность
        int totalWeight = 100;
        int randomWeight = random.nextInt(totalWeight);

        if (randomWeight < 50) return GameplayUnitsType.HOUSE;
        if (randomWeight < 75) return GameplayUnitsType.SHOP;
        if (randomWeight < 85) return GameplayUnitsType.CHURCH;
        if (randomWeight < 92) return GameplayUnitsType.FACTORY;
        return GameplayUnitsType.CITYHALL;
    }

    private void createNewBuilding(Point position, GameplayUnitsType type) {
        GameplayUnits newBuilding = new GameplayUnits(getWorld(), position.x, position.y, type);
        ((GameWorld)getWorld()).addGameplayUnits(newBuilding);

        // Уведомляем CityManager о новом здании
        CityManager cityManager = ((GameWorld)getWorld()).getCityManager();
        if (cityManager != null) {
            cityManager.onBuildingAdded(newBuilding);
        }
    }

    public float getExpansionChance() {
        // СИЛЬНО уменьшаем базовые шансы
        float baseChance = EXPANSION_CHANCES.getOrDefault(type, 0.001f) * 0.1f; // Уменьшаем в 10 раз

        // Усложняем условия
        float conditionModifier = (float) Math.pow(condition / 100.0f, 2); // Квадратичная зависимость
        long age = getWorld().getGameTime().getCurrentTimeMillis() - creationTime;
        float ageModifier = Math.min(1.0f, age / 7200000f); // 2 часа для максимального шанса

        // Учитываем плотность застройки - чем плотнее, тем меньше шанс
        float densityModifier = calculateDensityModifier();

        return baseChance * conditionModifier * ageModifier * densityModifier;
    }
    private float calculateDensityModifier() {
        // Проверяем плотность зданий вокруг
        int nearbyBuildings = 0;
        List<GameplayUnits> allBuildings = ((GameWorld)getWorld()).getGameplayUnits();

        for (GameplayUnits building : allBuildings) {
            double distance = Math.sqrt(Math.pow(x - building.getX(), 2) + Math.pow(y - building.getY(), 2));
            if (distance < 10) { // В радиусе 10 клеток
                nearbyBuildings++;
            }
        }

        // Чем больше зданий вокруг, тем меньше шанс расширения
        return Math.max(0.1f, 1.0f - (nearbyBuildings / 20.0f));
    }


    private void destroyBuilding() {
        CityManager cityManager = ((GameWorld)getWorld()).getCityManager();
        if (cityManager != null) {
            cityManager.onBuildingRemoved(this);
        }
        ((GameWorld)getWorld()).removeGameplayUnits(this);
    }
    public int getExpansionRadius() {
        return EXPANSION_RADIUS.getOrDefault(type, 2);
    }
    public void renovate() {
        isAbandoned = false;
        condition = 80 + getWorld().rand.nextInt(20);
    }
    public long getCreationTime() { return creationTime; }
    public int getCondition() { return condition; }
    public boolean isAbandoned() { return isAbandoned; }
    public GameplayUnitsType getType() {
        return type;
    }
    private static void createFallbackIcons() {
        // Создаем простые иконки программно, если загрузка не удалась
        icons.put(GameplayUnitsType.PORT, createSimpleIcon(Color.MAGENTA, 'P'));
        icons.put(GameplayUnitsType.AIRPORT, createSimpleIcon(Color.MAGENTA, 'A'));
        icons.put(GameplayUnitsType.MUSEUM, createSimpleIcon(Color.MAGENTA, 'M'));
        icons.put(GameplayUnitsType.HOUSE_CULTURE, createSimpleIcon(Color.MAGENTA,'D'));
        icons.put(GameplayUnitsType.CHURCH, createSimpleIcon(Color.MAGENTA,'C'));
        icons.put(GameplayUnitsType.CITYHALL, createSimpleIcon(Color.MAGENTA,'C'));
        icons.put(GameplayUnitsType.SHOP, createSimpleIcon(Color.MAGENTA,'S'));
        icons.put(GameplayUnitsType.FACTORY, createSimpleIcon(Color.MAGENTA,'F'));
    }
    private static BufferedImage createSimpleIcon(Color bgColor, char symbol) {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(bgColor);
        g.fillRect(0, 0, 32, 32);

        g.setColor(getContrastColor(bgColor));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int x = (32 - fm.charWidth(symbol)) / 2;
        int y = (32 - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(String.valueOf(symbol), x, y);

        g.dispose();
        return img;
    }

    @Override
    public void draw(Graphics2D g, int offsetX, int offsetY, float zoom) {
        int baseCellSize = 32;
        int drawX = (int)((x * baseCellSize + offsetX) * zoom);
        int drawY = (int)((y * baseCellSize + offsetY) * zoom);
        int size = (int)(baseCellSize * zoom);

        // Фон
     //   g.setColor(type.getColor());
   //     g.fillRect(drawX, drawY, size, size);

        // Рисуем иконку
        BufferedImage icon = icons.get(type);
        if (icon != null) {
            int iconSize = (int)(size * 1); // 80% от размера клетки
            int iconX = drawX + (size - iconSize) / 2;
            int iconY = drawY + (size - iconSize) / 2;
            g.drawImage(icon, iconX, iconY, iconSize, iconSize, null);
        }

        if (isSelected()) {
            g.setColor(Color.YELLOW);
            g.drawRect(drawX, drawY, size-1, size-1);
        }
    }
    private static Color getContrastColor(Color color) {
        int brightness = (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000;
        return brightness > 128 ? Color.BLACK : Color.WHITE;
    }
}