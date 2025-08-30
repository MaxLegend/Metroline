package metroline.core.world.tiles;

import metroline.objects.gameobjects.GameObject;
import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.StationLabel;
import metroline.objects.gameobjects.Station;

/**
 * Game tile that can contain game objects
 * @author Tesmio
 */
public class GameTile extends Tile {

    private transient GameObject content;
    public GameTile() {
        super((short) 0, (short) 0, (byte) 16); // Значения по умолчанию
    }
    public GameTile(short x, short y) {
        super(x, y, (byte) 16);
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
        } else if (content instanceof StationLabel) {
            content = world.getLabelAt(getX(), getY());
        }
    }
}

