package game.tiles;

import game.GameObject;
import game.Tile;

/**
 * Game tile that can contain game objects
 */
public class GameTile extends Tile {
    private GameObject content;

    public GameTile(int x, int y) {
        super(x, y);
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

