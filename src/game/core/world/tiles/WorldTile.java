package game.core.world.tiles;

import game.core.Tile;
import screens.WorldSandboxScreen;

import java.awt.*;
import java.io.Serializable;

/**
 * World tile that represents terrain with building permissions
 * @author Tesmio
 */
public class WorldTile extends Tile implements Serializable {
    private float perm; // 0 = can build, 1 = cannot build
    private static final long serialVersionUID = 1L;
    private Color baseTileColor = new Color(110, 110, 110);

    public WorldTile() {
        super(0, 0, 16);
    }
    public WorldTile(int x, int y) {
        super(x, y, 16);
    }
    public WorldTile(int x, int y, float perm) {
        super(x, y, 16);
        this.perm = perm;
        this.baseTileColor = new Color(110, 110, 110);
    }

    public WorldTile getWorldTile() {
        return new WorldTile(getX(), getY());
    }
    public Color getCurrentColor() {
        int range = 50; // Должно совпадать с тем, что используется в draw()
        int red = Math.max(0, Math.min(255, baseTileColor.getRed() - (int)(perm * range)));
        int green = Math.max(0, Math.min(255, baseTileColor.getGreen() - (int)(perm * range)));
        int blue = Math.max(0, Math.min(255, baseTileColor.getBlue() - (int)(perm * range)));
        return new Color(red, green, blue);
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

    public void setBaseTileColor(Color color) {
        this.baseTileColor = color != null ? color : Color.BLACK;
    }
    public Color getBaseTileColor() {
        return baseTileColor;
    }
    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        super.draw(g, offsetX, offsetY, zoom);

        int drawSize = (int)(size * zoom);
        int drawX = (int)((x * size + offsetX) * zoom);
        int drawY = (int)((y * size + offsetY) * zoom);

        int range = 50;    // Диапазон вариаций

        int red = Math.max(0, Math.min(255, baseTileColor.getRed() - (int)(perm * range)));
        int green = Math.max(0, Math.min(255, baseTileColor.getGreen() - (int)(perm * range)));
        int blue = Math.max(0, Math.min(255, baseTileColor.getBlue() - (int)(perm * range)));

        // Создаем основной цвет плитки
        Color tileColor = new Color(red, green, blue);
        g.setColor(tileColor);

        // Рисуем плитку с небольшим перекрытием
        g.fillRect(drawX - 1, drawY - 1, drawSize + 2, drawSize + 2);

        // В debug-режиме рисуем границы
        if (WorldSandboxScreen.getInstance().debugMode) {
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
