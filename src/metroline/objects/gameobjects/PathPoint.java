package metroline.objects.gameobjects;

import java.awt.*;

/**
 * Point in the tunnel path that uses world grid coordinates (like Station)
 */
public class PathPoint extends GameObject {
    public PathPoint(int x, int y) {
        super(x, y);
    }
    public PathPoint() {
        super(0, 0);
    }

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {

    }
}
