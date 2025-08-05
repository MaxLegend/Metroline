package game.core.world.tiles;

import game.core.Tile;
import screens.WorldGameScreen;

import java.awt.*;
import java.io.Serializable;

/**
 * World tile that represents terrain with building permissions
 */
public class WorldTile extends Tile {
    private float perm; // 0 = can build, 1 = cannot build
    public WorldTile() {
        super(0, 0, 16);
    }
    public WorldTile(int x, int y, float perm) {
        super(x, y, 16);
        this.perm = perm;
    }

    /**
     * Gets the building permission value
     * @return Permission value (0-1)
     */
    public float getPerm() { return perm; }

    /**
     * Sets the building permission value
     * @param perm New permission value (0-1)
     */
    public void setPerm(float perm) { this.perm = perm; }

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        super.draw(g, offsetX, offsetY, zoom);

        int drawSize = (int)(size * zoom);
        int drawX = (int)((x * size + offsetX) * zoom);
        int drawY = (int)((y * size + offsetY) * zoom);

        // Темная цветовая схема на основе уровня разрешения (perm)
        int baseDark = 110; // Базовый темный цвет
        int range = 50;    // Диапазон вариаций

        // Инвертируем значение perm для темной темы (0 = светлее, 1 = темнее)
        int darkValue = baseDark - (int)(perm * range);
        darkValue = Math.max(baseDark - range, Math.min(baseDark + range, darkValue));

        // Создаем основной цвет плитки
        Color tileColor = new Color(darkValue, darkValue, darkValue);
        g.setColor(tileColor);

        // Рисуем плитку с небольшим перекрытием
        g.fillRect(drawX - 1, drawY - 1, drawSize + 2, drawSize + 2);

        // В debug-режиме рисуем границы
        if (WorldGameScreen.getInstance().debugMode) {
            g.setColor(new Color(80, 80, 80)); // Темно-серые границы
            g.drawRect(drawX, drawY, drawSize, drawSize);

            // Дополнительная информация для отладки
            if (zoom > 1.5) { // Показываем только при достаточном увеличении
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.PLAIN, 10));
                g.drawString(String.format("%.1f", perm), drawX + 2, drawY + 12);
            }
        }
    }
}
