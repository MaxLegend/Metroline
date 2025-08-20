package metroline.core.world.tiles;

import java.awt.*;
import java.io.Serializable;

/**
 * Represents a basic tile in the game world
 * @author Tesmio
 */
public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;
    protected short x, y;
    protected byte size;

    protected Tile() {
        this((short) 0, (short) 0, (byte) 0); // Инициализация значениями по умолчанию
    }
    public Tile(short x, short y, byte size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }
    public Tile(short x, short y) {
        this.x = x;
        this.y = y;
    }
    /**
     * Gets the x coordinate of this tile
     * @return X coordinate
     */
    public short getX() { return x; }

    /**
     * Gets the y coordinate of this tile
     * @return Y coordinate
     */
    public short getY() { return y; }

    /**
     * Gets the size of this tile
     * @return Tile size in pixels
     */
    public byte getSize() { return size; }

    /**
     * Draws the tile on the graphics context
     * @param g Graphics context to draw on
     * @param offsetX Horizontal offset for drawing
     * @param offsetY Vertical offset for drawing
     * @param zoom Current zoom level
     */
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
    }
}
