package metroline.objects.gameobjects;

import metroline.core.world.World;
import metroline.objects.enums.GameplayUnitsType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameplayUnits extends GameObject {
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
            icons.put(GameplayUnitsType.BIG_HOUSE, loadIcon("big_house.png"));
            icons.put(GameplayUnitsType.HOUSE, loadIcon("house.png"));
            icons.put(GameplayUnitsType.SMALL_HOUSE, loadIcon("small_house.png"));
        } catch (IOException e) {
            e.printStackTrace();
            // Запасной вариант - создаем простые иконки
            createFallbackIcons();
        }
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
        // Здания slowly deteriorate over time
        if (!isAbandoned && getWorld().rand.nextFloat() < 0.01f) {
            condition = Math.max(0, condition - 1);

            // Если состояние слишком плохое, здание забрасывается
            if (condition < 30 && getWorld().rand.nextFloat() < 0.05f) {
                abandon();
            }
        }
    }
    private void abandon() {
        isAbandoned = true;
        // Можно изменить внешний вид заброшенного здания
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
            int iconSize = (int)(size * 0.8); // 80% от размера клетки
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