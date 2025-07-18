package game.tiles;

import game.Tile;

public class WorldTile extends Tile {
    private float perm; // Сложность строительства (0-1)

    public WorldTile(int x, int y, int size) {
        super(x, y, size);
        this.perm = 0f; // По умолчанию можно строить
    }

    public float getPerm() { return perm; }
    public void setPerm(float perm) {
        this.perm = Math.max(0, Math.min(1, perm));
    }
}
