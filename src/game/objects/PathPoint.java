package game.objects;

import game.core.GameObject;

import java.awt.*;

/**
 * Point in the tunnel path that uses world grid coordinates (like Station)
 */
public class PathPoint extends GameObject {
    public PathPoint(int x, int y) {
        super(x, y);
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
