package game.tiles;

import game.core.GameObject;
import game.core.Tile;

public class GameTileBig extends Tile {
    private GameObject content;

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
