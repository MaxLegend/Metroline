package metroline.objects.gameobjects;

import metroline.core.world.World;
import metroline.input.selection.Selectable;
import metroline.input.selection.SelectionManager;
import metroline.screens.worldscreens.normal.GameWorldScreen;

import java.awt.*;

/**
 * RiverPoint - a placeable game object that forms river paths.
 * Stored in gameGrid like stations. Multiple RiverPoints connected form a River.
 */
public class RiverPoint extends GameObject  {

    private River parentRiver;
    private int orderIndex; // Position in river's point list

    public RiverPoint() {
        super(0, 0);
    }

    public RiverPoint(int x, int y) {
        super(x, y);
        this.name = "river_point_" + getUniqueId();
    }

    public RiverPoint(World world, int x, int y) {
        super(x, y);
        this.setWorld(world);
        this.name = "river_point_" + getUniqueId();
    }

    public RiverPoint(World world, int x, int y, River parentRiver) {
        super(x, y);
        this.setWorld(world);
        this.parentRiver = parentRiver;
        this.name = "river_point_" + getUniqueId();
    }

    public River getParentRiver() {
        return parentRiver;
    }

    public void setParentRiver(River river) {
        this.parentRiver = river;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int index) {
        this.orderIndex = index;
    }


    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }

    /**
     * Move this point to new coordinates and recalculate parent river path
     */
    public void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        if (parentRiver != null) {
            parentRiver.recalculatePath();
        }
    }

    @Override
    public void draw(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        // Only draw RiverPoint in debug mode
        if (getWorld() != null && getWorld().getWorldScreen() instanceof GameWorldScreen s) {
            if (!s.debugMode) {
                return; // Skip rendering in normal mode
            }
        }

        // Debug mode visualization
        int screenX = (int)((x * 32 + offsetX + 16) * zoom);
        int screenY = (int)((y * 32 + offsetY + 16) * zoom);
        int size = (int)(14 * zoom);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Selected = yellow, normal = blue
        if (isSelected()) {
            g2d.setColor(new Color(255, 255, 100, 220));
            size = (int)(16 * zoom);
        } else {
            g2d.setColor(new Color(80, 150, 220, 200));
        }

        // Fill circle
        g2d.fillOval(screenX - size/2, screenY - size/2, size, size);

        // Border
        g2d.setColor(new Color(40, 80, 120));
        g2d.setStroke(new BasicStroke(2f * zoom));
        g2d.drawOval(screenX - size/2, screenY - size/2, size, size);

        // Draw order number
        if (parentRiver != null && parentRiver.getPoints().size() > 1) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, (int)(10 * zoom)));
            String num = String.valueOf(orderIndex + 1);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = screenX - fm.stringWidth(num) / 2;
            int textY = screenY + fm.getAscent() / 2 - 1;
            g2d.drawString(num, textX, textY);
        }
    }
}
