package game.objects;

import game.core.GameObject;

import java.awt.*;
import java.io.Serializable;

/**
 * Point in the tunnel path that uses world grid coordinates (like Station)
 */
public class PathPoint extends GameObject implements Serializable {
    private static final long serialVersionUID = 1L;
    public PathPoint(int x, int y) {
        super(x, y);
    }
    public PathPoint() {
        super(0, 0);
    }
    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        // Optional: можно реализовать отрисовку точки, если нужно


        if (selected) {
            int drawX = (int)((getX() * 32 + offsetX + 16) * zoom);
            int drawY = (int)((getY() * 32 + offsetY + 16) * zoom);
            g.setColor(Color.BLACK);
            g.fillOval(drawX - 3, drawY - 3, 6, 6);
        }
    }
}
