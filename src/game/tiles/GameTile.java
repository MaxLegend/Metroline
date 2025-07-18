package game.tiles;

import game.GameObject;
import game.Tile;

public class GameTile extends Tile {
    private GameObject object;

    public GameTile(int x, int y, int size) {
        super(x, y, size);
        this.object = null;
    }

    public boolean hasGameObject() { return object != null; }
    public GameObject getGameObject() { return object; }
    public void setGameObject(GameObject object) { this.object = object; }
    public void clearGameObject() { this.object = null; }
}
