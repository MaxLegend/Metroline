package game.tiles;

import game.core.Tile;

import java.awt.*;
/**
 * World tile that represents terrain with building permissions
 */
public class WorldTile extends Tile {
    private float perm; // 0 = can build, 1 = cannot build

    public WorldTile(int x, int y, float perm) {
        super(x, y, 16);
        this.perm = perm;
    }

    /**
     * Gets the building permission value
     * @return Permission value (0-1)
     */
    public float getPerm() { return perm; }

    /**
     * Sets the building permission value
     * @param perm New permission value (0-1)
     */
    public void setPerm(float perm) { this.perm = perm; }

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        super.draw(g, offsetX, offsetY, zoom);

        int drawSize = (int)(size * zoom);
        int drawX = (int)((x * size + offsetX) * zoom);
        int drawY = (int)((y * size + offsetY) * zoom);

        // Color based on permission value
        int grayValue = 230 - (int)(perm * 100);
        grayValue = Math.max(100, Math.min(230, grayValue));
        g.setColor(new Color(grayValue, grayValue, grayValue));
        g.fillRect(drawX, drawY, drawSize, drawSize);

        // Draw border
   //     g.setColor(Color.GRAY);
    //    g.drawRect(drawX, drawY, drawSize, drawSize);
    }
}
