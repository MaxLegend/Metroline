package game.core.world.tiles;

import game.core.GameObject;
import game.core.Tile;

import java.io.Serializable;
/**
 * Big game tile that can contain game objects
 * @author Tesmio
 */
public class GameTileBig extends Tile {
    private GameObject content;
    public GameTileBig() {
        super(0, 0, 64); // Значения по умолчанию
    }
    public GameTileBig(int x, int y) {
        super(x, y, 64);
    }
    /**
     * Gets the game object in this tile
     * @return GameObject or null if empty
     */
    public GameObject getContent() { return content; }

    /**
     * Sets the game object in this tile
     * @param content GameObject to place or null to clear
     */
    public void setContent(GameObject content) { this.content = content; }

    /**
     * Checks if this tile is empty
     * @return True if no game object is present
     */
    public boolean isEmpty() { return content == null; }
}
