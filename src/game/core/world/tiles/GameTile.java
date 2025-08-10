package game.core.world.tiles;

import game.core.GameObject;
import game.core.Tile;
import game.core.world.GameWorld;
import game.objects.Label;
import game.objects.Station;
import util.MetroLogger;

import java.io.Serializable;

/**
 * Game tile that can contain game objects
 * @author Tesmio
 */
public class GameTile extends Tile {

    private transient GameObject content;
    public GameTile() {
        super(0, 0, 16); // Значения по умолчанию
    }
    public GameTile(int x, int y) {
        super(x, y, 16);
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
    public void setContent(GameObject content) {

        this.content = content;
    }

    /**
     * Checks if this tile is empty
     * @return True if no game object is present
     */
    public boolean isEmpty() { return content == null; }

    public GameTile getGameTile() {
        return new GameTile(getX(), getY());
    }
    public void restoreContent(GameWorld world) {
        if (content instanceof Station) {
            content = world.getStationAt(getX(), getY());
        } else if (content instanceof Label) {
            content = world.getLabelAt(getX(), getY());
        }
    }
}

