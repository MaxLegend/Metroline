package metroline.objects.gameobjects;

import metroline.input.selection.SelectionManager;

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
    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }
    @Override
    public void draw(Graphics2D g, int offsetX, int offsetY, float zoom) {

    }
}
