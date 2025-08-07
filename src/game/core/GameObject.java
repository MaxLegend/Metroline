package game.core;

import java.awt.*;
import java.io.Serializable;

/**
 * Base class for all game objects
 * @author Tesmio
 */
public abstract class GameObject implements Serializable {
    private static final long serialVersionUID = 1L;
    public int x;
    public int y;
    public boolean selected = false;
    public GameObject() {
        super();
    }
    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }
    /**
     * Gets the Tile of this coord
     * @return Tile
     */
    public Tile getTile(int xTile, int yTile) {
        return new Tile(xTile,yTile);
    }

    /**
     * Gets the x coordinate of this object
     * @return X coordinate
     */
    public int getX() { return x; }

    /**
     * Gets the y coordinate of this object
     * @return Y coordinate
     */
    public int getY() { return y; }

    /**
     * Checks if this object is selected
     * @return True if selected
     */
    public boolean isSelected() { return selected; }

    /**
     * Sets the selected state of this object
     * @param selected New selected state
     */
    public void setSelected(boolean selected) { this.selected = selected; }

    /**
     * Draws the game object
     * @param g Graphics context
     * @param offsetX Horizontal offset
     * @param offsetY Vertical offset
     * @param zoom Current zoom level
     */
    public abstract void draw(Graphics g, int offsetX, int offsetY, float zoom);
}
