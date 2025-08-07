package game.core;

import java.awt.*;

/**
 * Represents a basic tile in the game world
 * @author Tesmio
 */
public class FloatTile {
    protected float x, y;
    protected float size;

    public FloatTile(float x, float y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }
    public FloatTile(float x, float y) {
        this.x = x;
        this.y = y;
    }
    /**
     * Gets the x coordinate of this tile
     * @return X coordinate
     */
    public float getX() { return x; }

    /**
     * Gets the y coordinate of this tile
     * @return Y coordinate
     */
    public float getY() { return y; }

    /**
     * Gets the size of this tile
     * @return Tile size in pixels
     */
    public float getSize() { return size; }

    /**
     * Draws the tile on the graphics context
     * @param g Graphics context to draw on
     * @param offsetX Horizontal offset for drawing
     * @param offsetY Vertical offset for drawing
     * @param zoom Current zoom level
     */
    public void draw(Graphics g, float offsetX, float offsetY, float zoom) {
        int drawSize = (int)(size * zoom);
        int drawX = (int)((x * size + offsetX) * zoom);
        int drawY = (int)((y * size + offsetY) * zoom);

        g.setColor(new Color(230, 230, 230));
        g.fillRect(drawX, drawY, drawSize, drawSize);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(drawX, drawY, drawSize, drawSize);
    }
}
