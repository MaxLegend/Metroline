package game.core;

import java.awt.*;
import java.io.Serializable;

/**
 * Represents a basic tile in the game world
 */
public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;
    protected int x, y;
    protected int size;
    protected Tile() {
        this(0, 0, 0); // Инициализация значениями по умолчанию
    }
    public Tile(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }
    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }
    /**
     * Gets the x coordinate of this tile
     * @return X coordinate
     */
    public int getX() { return x; }

    /**
     * Gets the y coordinate of this tile
     * @return Y coordinate
     */
    public int getY() { return y; }

    /**
     * Gets the size of this tile
     * @return Tile size in pixels
     */
    public int getSize() { return size; }

    /**
     * Draws the tile on the graphics context
     * @param g Graphics context to draw on
     * @param offsetX Horizontal offset for drawing
     * @param offsetY Vertical offset for drawing
     * @param zoom Current zoom level
     */
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(size * zoom);
        int drawX = (int)((x * size + offsetX) * zoom);
        int drawY = (int)((y * size + offsetY) * zoom);

        g.setColor(new Color(230, 230, 230));
        g.fillRect(drawX, drawY, drawSize, drawSize);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(drawX, drawY, drawSize, drawSize);
    }
}
