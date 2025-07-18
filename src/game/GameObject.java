package game;

import java.awt.*;

public abstract class GameObject {
    protected int x, y; // Координаты в сетке
    protected Color color;
    protected boolean selected;

    public GameObject(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.selected = false;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public abstract void update();
}
