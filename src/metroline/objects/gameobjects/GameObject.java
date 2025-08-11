package metroline.objects.gameobjects;

import metroline.core.world.tiles.Tile;
import metroline.core.world.World;

import java.awt.*;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base class for all game objects
 * @author Tesmio
 */
public abstract class GameObject implements Serializable {
    private static final long serialVersionUID = 1L;
    public int x;
    public int y;
    public boolean selected = false;
    private World world;

    // Генератор уникальных ID для всех объектов
    private static final AtomicLong idGenerator = new AtomicLong(0);

    // Уникальный идентификатор объекта
    private long uniqueId;

    public GameObject() {
        super();
        this.uniqueId = idGenerator.incrementAndGet();
    }

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
        this.uniqueId = idGenerator.incrementAndGet();
    }

    public GameObject(World world, int x, int y) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.uniqueId = idGenerator.incrementAndGet();
    }
    /**
     * Получает уникальный идентификатор объекта
     * @return уникальный ID
     */
    public long getUniqueId() {
        return uniqueId;
    }
    public void setUniqueId(long uniqueId) {
        this.uniqueId = uniqueId;
    }
    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GameObject that = (GameObject) obj;
        return uniqueId == that.uniqueId;
    }
    @Override
    public int hashCode() {
        return Long.hashCode(uniqueId);
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
