package screens;

import game.GameObject;
import game.World;
import game.dialog.ColorSelDialog;
import game.objects.Station;
import game.tiles.GameTile;
import game.tiles.WorldTile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

public class WorldScreen extends GameScreen {
    private World world;
    private float zoom = 1.0f;
    private Point viewOffset = new Point(0, 0);
    private Point lastMousePos = new Point(0, 0);


    // Режимы работы
    private boolean buildStationMode = false;
    private boolean buildTunnelMode = false;
    private Station firstSelectedStation = null;

    private List<TunnelData> tunnels = new ArrayList<>();


    public WorldScreen(MainFrame frame) {
        super(frame);
        world = new World(100, 100, 32);

        setupInputListeners();
    }

    private void setupInputListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();

                if (SwingUtilities.isRightMouseButton(e)) {
                    // Начало перетаскивания карты
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    handleLeftClick(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Завершение перетаскивания
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Перетаскивание карты
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;
                    viewOffset.translate(dx, dy);
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }
        });

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Масштабирование относительно курсора
                float zoomFactor = e.getWheelRotation() < 0 ? 1.1f : 0.9f;
                Point mousePos = e.getPoint();
                Point worldPos = screenToWorld(mousePos);

                zoom *= zoomFactor;
                zoom = Math.max(0.1f, Math.min(5.0f, zoom));

                Point newScreenPos = worldToScreen(worldPos);
                viewOffset.translate(mousePos.x - newScreenPos.x, mousePos.y - newScreenPos.y);

                repaint();
            }
        });
    }

    /**
     * Обработка левого клика мыши
     */
    private void handleLeftClick(MouseEvent e) {
        Point worldPos = screenToWorld(e.getPoint());
        int tileX = worldPos.x / world.getTileSize();
        int tileY = worldPos.y / world.getTileSize();

        if (tileX >= 0 && tileX < world.getWidth() &&
                tileY >= 0 && tileY < world.getHeight()) {

            if (buildStationMode) {
                toggleStation(tileX, tileY);
            } else if (buildTunnelMode) {
                handleTunnelBuilding(tileX, tileY);
            }
        }
    }

    /**
     * Создание/удаление станции
     */
    private void toggleStation(int x, int y) {
        GameTile tile = world.getGameLayer()[x][y];
        if (tile.hasGameObject() && tile.getGameObject() instanceof Station) {
            // Удаляем станцию
            tile.clearGameObject();
        } else {
            // Создаем новую станцию
            showColorSelectionDialog(x, y);
        }
        repaint();
    }

    /**
     * Показ диалога выбора цвета
     */
    private void showColorSelectionDialog(int x, int y) {
        ColorSelDialog dialog = new ColorSelDialog(frame, x, y, world);
        dialog.setVisible(true);
    }

    /**
     * Обработка строительства туннеля
     */
    private void handleTunnelBuilding(int x, int y) {
        GameTile tile = world.getGameLayer()[x][y];
        if (tile.hasGameObject() && tile.getGameObject() instanceof Station) {
            Station station = (Station) tile.getGameObject();

            if (firstSelectedStation == null) {
                firstSelectedStation = station;
                station.setSelected(true);
            } else if (!firstSelectedStation.equals(station)) {
                if (firstSelectedStation.canConnect(station)) {
                    // Проверяем, нет ли уже соединения
                    if (!stationsAlreadyConnected(firstSelectedStation, station)) {
                        TunnelData tunnel = new TunnelData(
                                firstSelectedStation,
                                station,
                                firstSelectedStation.getColor()
                        );
                        tunnels.add(tunnel);

                        // Добавляем соединения в обе стороны
                        firstSelectedStation.addConnection(station);
                        station.addConnection(firstSelectedStation);
                    }
                }
                firstSelectedStation.setSelected(false);
                firstSelectedStation = null;
            }
        }
        repaint();
    }
    private boolean stationsAlreadyConnected(Station s1, Station s2) {
        for (TunnelData tunnel : tunnels) {
            if ((tunnel.getStartStation() == s1 && tunnel.getEndStation() == s2) ||
                    (tunnel.getStartStation() == s2 && tunnel.getEndStation() == s1)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Преобразование экранных координат в мировые
     */
    private Point screenToWorld(Point screenPoint) {
        return new Point(
                (int) ((screenPoint.x - viewOffset.x) / zoom),
                (int) ((screenPoint.y - viewOffset.y) / zoom)
        );
    }

    /**
     * Преобразование мировых координат в экранные
     */
    private Point worldToScreen(Point worldPoint) {
        return new Point(
                (int) (worldPoint.x * zoom + viewOffset.x),
                (int) (worldPoint.y * zoom + viewOffset.y)
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Применяем масштаб и смещение
        g2d.scale(zoom, zoom);
        g2d.translate(viewOffset.x / zoom, viewOffset.y / zoom);

        // Отрисовка мира
        drawWorld(g2d);
        drawTunnels(g2d, world.getTileSize());
        drawGameObjects(g2d);

        // Возвращаем трансформацию
        g2d.dispose();
    }

    /**
     * Отрисовка базового слоя мира
     */
    private void drawWorld(Graphics2D g) {
        WorldTile[][] worldLayer = world.getWorldLayer();
        int tileSize = world.getTileSize();

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                WorldTile tile = worldLayer[x][y];

                // Цвет зависит от значения perm
                float perm = tile.getPerm();
                Color color = new Color(
                        240 - (int)(perm * 100),
                        240 - (int)(perm * 100),
                        240 - (int)(perm * 100)
                );

                g.setColor(color);
                g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);

                // Границы клеток
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
            }
        }
    }

    /**
     * Отрисовка игровых объектов
     */
    private void drawGameObjects(Graphics2D g) {
        GameTile[][] gameLayer = world.getGameLayer();
        int tileSize = world.getTileSize();

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                GameTile tile = gameLayer[x][y];
                if (tile.hasGameObject() && tile.getGameObject() instanceof Station) {
                    drawStation(g, (Station) tile.getGameObject(), tileSize);
                }
            }
        }
    }

    /**
     * Отрисовка станции
     */
    private void drawStation(Graphics2D g, Station station, int tileSize) {
        int size = tileSize / 2;
        int offset = (tileSize - size) / 2;
        int x = station.getX() * tileSize + offset;
        int y = station.getY() * tileSize + offset;

        // Основной цвет
        g.setColor(station.getColor());
        g.fillRect(x, y, size, size);

        // Выделение
        if (station.isSelected()) {
            g.setColor(Color.YELLOW);
            g.drawRect(x - 1, y - 1, size + 2, size + 2);
        }

        // Обводка для пересадочных
        if (station.isTransfer()) {
            g.setColor(Color.BLACK);
            g.drawRect(x - 2, y - 2, size + 4, size + 4);
        }
    }

    /**
     * Отрисовка туннеля
     */
    private void drawTunnels(Graphics2D g, int tileSize) {
        for (TunnelData tunnel : tunnels) {
            drawSingleTunnel(g, tunnel, tileSize);
        }
    }
    private void drawSingleTunnel(Graphics2D g, TunnelData tunnel, int tileSize) {
        g.setColor(tunnel.getColor());
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        List<Point> path = tunnel.getPath();
        if (path.size() < 2) return;

        // Рисуем плавные кривые для изгибов
        if (path.size() == 3) {
            Point p1 = path.get(0);
            Point p2 = path.get(1);
            Point p3 = path.get(2);

            int x1 = p1.x * tileSize + tileSize/2;
            int y1 = p1.y * tileSize + tileSize/2;
            int x2 = p2.x * tileSize + tileSize/2;
            int y2 = p2.y * tileSize + tileSize/2;
            int x3 = p3.x * tileSize + tileSize/2;
            int y3 = p3.y * tileSize + tileSize/2;

            // Плавный угол
            if (x1 == x2) { // Вертикальная -> горизонтальная
                g.drawLine(x1, y1, x1, (y1+y2)/2);
                g.drawLine(x1, (y1+y2)/2, x2, y2);
                g.drawLine(x2, y2, x3, y3);
            } else { // Горизонтальная -> вертикальная
                g.drawLine(x1, y1, (x1+x2)/2, y1);
                g.drawLine((x1+x2)/2, y1, x2, y2);
                g.drawLine(x2, y2, x3, y3);
            }

        }
        else if (path.size() == 2) {
            Point p1 = path.get(0);
            Point p2 = path.get(1);
            g.drawLine(
                    p1.x * tileSize + tileSize/2,
                    p1.y * tileSize + tileSize/2,
                    p2.x * tileSize + tileSize/2,
                    p2.y * tileSize + tileSize/2
            );
        }

        g.setStroke(oldStroke);
    }
    @Override
    public void update() {
        // Обновление состояния игры
    }

    public boolean isBuildStationMode() {
        return buildStationMode;
    }

    public void setBuildStationMode(boolean buildStationMode) {
        this.buildStationMode = buildStationMode;
        this.buildTunnelMode = !buildStationMode;
    }

    public boolean isBuildTunnelMode() {
        return buildTunnelMode;
    }

    public void setBuildTunnelMode(boolean buildTunnelMode) {
        this.buildTunnelMode = buildTunnelMode;
        this.buildStationMode = !buildTunnelMode;
    }

    public Station getFirstSelectedStation() {
        return firstSelectedStation;
    }

    public void setFirstSelectedStation(Station firstSelectedStation) {
        this.firstSelectedStation = firstSelectedStation;
    }

    public class TunnelData {
        private Station startStation;
        private Station endStation;
        private Color color;
        private List<Point> path;

        private boolean isBendPointSelected = false;

        public TunnelData(Station start, Station end, Color color) {
            this.startStation = start;
            this.endStation = end;
            this.color = color;
            this.path = calculateOptimalPath();
        }


        private List<Point> calculateOptimalPath() {
            List<Point> path = new ArrayList<>();
            int startX = startStation.getX();
            int startY = startStation.getY();
            int endX = endStation.getX();
            int endY = endStation.getY();

            // Центральные точки станций
            path.add(new Point(startX, startY));

            int dx = endX - startX;
            int dy = endY - startY;

            // Проверяем возможность прямого диагонального соединения
            if (Math.abs(dx) == Math.abs(dy)) {
                // Прямая диагональ без изгибов
                path.add(new Point(endX, endY));
            } else {
                // Определяем основные направления
                int dirX = Integer.compare(dx, 0);
                int dirY = Integer.compare(dy, 0);

                // Проверяем существующие соединения стартовой станции
                if (startStation.hasConnections()) {
                    Point lastDir = startStation.getLastConnectionDirection();

                    // Если последнее соединение было диагональным
                    if (Math.abs(lastDir.x) == Math.abs(lastDir.y) && Math.abs(lastDir.x) == 1) {
                        // Продолжаем в том же направлении насколько возможно
                        int extendSteps = Math.min(Math.abs(dx), Math.abs(dy));
                        int extendX = startX + lastDir.x * extendSteps;
                        int extendY = startY + lastDir.y * extendSteps;

                        path.add(new Point(extendX, extendY));

                        // Обновляем оставшееся расстояние
                        dx = endX - extendX;
                        dy = endY - extendY;
                        dirX = Integer.compare(dx, 0);
                        dirY = Integer.compare(dy, 0);
                    }
                }

                // Строим оставшийся путь с учетом возможного продолжения
                if (Math.abs(dx) > Math.abs(dy)) {
                    // Первый изгиб по X, затем по Y
                    int midX = startX + dx - (dirX * Math.abs(dy));
                    // Проверяем, не добавили ли мы уже точку продолжения
                    if (path.size() == 1) {
                        path.add(new Point(midX, startY));
                    }
                    path.add(new Point(endX, endY));
                } else {
                    // Первый изгиб по Y, затем по X
                    int midY = startY + dy - (dirY * Math.abs(dx));
                    // Проверяем, не добавили ли мы уже точку продолжения
                    if (path.size() == 1) {
                        path.add(new Point(startX, midY));
                    }
                    path.add(new Point(endX, endY));
                }
            }

            return path;
        }

        public List<Point> getPath() { return path; }
        public Color getColor() { return color; }
        public Station getStartStation() { return startStation; }
        public Station getEndStation() { return endStation; }
    }
}
