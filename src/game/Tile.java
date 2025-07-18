package game;

public class Tile {
    protected int x, y; // Координаты в сетке
    protected int size; // Размер в пикселях

    public Tile(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getSize() { return size; }
}
