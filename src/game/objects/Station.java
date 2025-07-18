package game.objects;

import game.GameObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
public class Station extends GameObject {
    private boolean isTransfer; // Является ли пересадочной
    private Station[] connections; // Соединения с другими станциями
    private List<Point> lastConnectionPath;
    public Station(int x, int y, Color color) {
        super(x, y, color);
        this.isTransfer = false;
        this.connections = new Station[8]; // 8 направлений
        this.lastConnectionPath = new ArrayList<>();
    }
    /**
     * Проверяет, есть ли у станции соединения с другими станциями
     * @return true если есть хотя бы одно соединение
     */
    public boolean hasConnections() {
        for (Station s : connections) {
            if (s != null) return true;
        }
        return false;
    }
    /**
     * Возвращает направление последнего сегмента последнего соединения
     * @return Point с направлением (x, y) где значения -1, 0 или 1
     */
    public Point getLastConnectionDirection() {
        if (lastConnectionPath.size() < 2) {
            return new Point(0, 0);
        }

        // Берем последние две точки пути
        Point lastPoint = lastConnectionPath.get(lastConnectionPath.size() - 1);
        Point prevPoint = lastConnectionPath.get(lastConnectionPath.size() - 2);

        // Вычисляем направление
        int dirX = Integer.compare(lastPoint.x - prevPoint.x, 0);
        int dirY = Integer.compare(lastPoint.y - prevPoint.y, 0);

        return new Point(dirX, dirY);
    }
    /**
     * Обновляет информацию о последнем построенном пути соединения
     * @param path список точек пути соединения
     */
    public void setLastConnectionPath(List<Point> path) {
        this.lastConnectionPath = new ArrayList<>(path);
    }
    public boolean isTransfer() { return isTransfer; }
    public void setTransfer(boolean transfer) { isTransfer = transfer; }

    public boolean canConnect(Station other) {
        return this.color.equals(other.color);
    }

    public boolean addConnection(Station station) {
        if (!canConnect(station)) return false;

        // Определяем направление соединения (0-7)
        int direction = calculateDirection(station);

        if (connections[direction] == null) {
            connections[direction] = station;
            return true;
        }
        return false;
    }
    private int calculateDirection(Station other) {
        int dx = other.getX() - this.x;
        int dy = other.getY() - this.y;

        if (dx == 0 && dy < 0) return 0; // Север
        if (dx > 0 && dy < 0) return 1;  // Северо-восток
        if (dx > 0 && dy == 0) return 2; // Восток
        if (dx > 0 && dy > 0) return 3;  // Юго-восток
        if (dx == 0 && dy > 0) return 4; // Юг
        if (dx < 0 && dy > 0) return 5;  // Юго-запад
        if (dx < 0 && dy == 0) return 6; // Запад
        if (dx < 0 && dy < 0) return 7;  // Северо-запад

        return 0; // По умолчанию
    }

    @Override
    public void update() {
        // Проверяем, стала ли станция пересадочной
        isTransfer = false;
        for (Station s : connections) {
            if (s != null && !s.color.equals(this.color)) {
                isTransfer = true;
                break;
            }
        }
    }
}
