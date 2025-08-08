package game.objects;

import game.core.GameObject;
import game.core.GameTime;
import game.core.world.GameWorld;
import game.core.world.World;
import game.objects.enums.Direction;
import game.objects.enums.StationType;
import screens.WorldSandboxScreen;
import screens.WorldScreen;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;


/**
 * Station game object
 */
public class Station extends GameObject {
    public static final Color[] COLORS = {
            new Color(157, 6, 6),
            new Color(0, 100, 0),
            new Color(0, 120, 190),
            new Color(27, 57, 208),
            new Color(25, 149, 176),
            new Color(110, 63, 21),
            new Color(200, 100, 0),
            new Color(133, 7, 133),
            new Color(211, 179, 8),
            new Color(153, 153, 153),
            new Color(153, 204, 0),
            new Color(79, 155, 155),
            new Color(201, 48, 128),
            new Color(3, 121, 95),
            new Color(148, 21, 73),
            new Color(109, 148, 104),
    };

    private long buildStartTime; // Игровое время начала строительства
    private long buildDuration = 5 * 60 * 1000; // 5 минут игрового времени


    private String name;

    private static final String[] NAME_PARTS = {"Pyatorocka", "Magnit", "Nizhni", "Kotova", "Baton", "Butilka",
            "Lolkek", "Tesmio", "Geroyev", "Svetofor", "Ploshad", "Proezd",
            "Sobakov", "Balka", "Ulitca", "Bred"};

    private Color color;
    private StationType type;
    private Map<Direction, Station> connections = new EnumMap<>(Direction.class);
    public Station() {
        super(0, 0);
    }
    public Station(World world, int x, int y, Color color, StationType type) {
        super(x, y);
        this.setWorld(world);
        this.color = color;
        this.type = type;
        this.name = generateRandomName();
    }
    public void setBuildStartTime(long gameTime) {
        this.buildStartTime = gameTime;
    }

    public void setBuildDuration(long duration) {
        this.buildDuration = duration;
    }

    public boolean isConstructionComplete(long currentGameTime) {
        if (type != StationType.BUILDING) return false;
        return currentGameTime - buildStartTime >= buildDuration;
    }

    public float getConstructionProgress(long currentGameTime) {
        if (type != StationType.BUILDING) return 1.0f;
        return Math.min(1.0f, (float)(currentGameTime - buildStartTime) / buildDuration);
    }

    private String generateRandomName() {
        Random rand = new Random();
        return NAME_PARTS[rand.nextInt(NAME_PARTS.length)];
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        // Обновляем метку, если она существует
        Label label = getWorld().getLabelForStation(this);
        if (label != null) {
            label.setText(name);
        }
    }
/**
     * Gets the station color
     * @return Color of the station
     */
    public Color getColor() { return color; }

    /**
     * Sets the station color
     * @param color New color
     */
    public void setColor(Color color) { this.color = color; }

    /**
     * Gets the station type
     * @return Station type
     */
    public StationType getType() { return type; }

    /**
     * Sets the station type
     * @param type New station type
     */
    public void setType(StationType type, GameTime gameTime) {
        if (type == StationType.BUILDING && this.type != StationType.BUILDING) {
            this.buildStartTime = gameTime.getCurrentTimeMillis();
        }
        this.type = type;
        // Обновляем метку, если она существует
        Label label = getWorld().getLabelForStation(this);
        if (label != null) {
            label.setText(name);
        }

        // Уведомляем связанные туннели об изменении
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).updateConnectedTunnels(this);
        }
    }
    /**
     * Gets connected stations with their directions
     * @return Map of directions to connected stations
     */
    public Map<Direction, Station> getConnections() { return connections; }
    public boolean connect(Station other) {
        Direction dir = getDirectionTo(other);
        Direction oppositeDir = dir.getOpposite();

        // Check if we can make this connection
        if (connections.size() >= 2) return false;
        if (connections.containsKey(oppositeDir)) return false;

        // Check if other station can accept this connection
        if (other.connections.size() >= 2) return false;
        if (other.connections.containsKey(dir)) return false;
        if(other.getColor() != this.getColor()) return false;
        // Make the connection
        connections.put(oppositeDir, other);
        other.connections.put(dir, this);

        // Update types for both stations
        if (this.type != StationType.PLANNED && this.type != StationType.BUILDING) {
            this.updateType();
        }
        if (other.type != StationType.PLANNED && other.type != StationType.BUILDING) {
            other.updateType();
        }

        return true;
    }

    public void disconnect(Station other) {
        Direction dirToRemove = null;
        for (Map.Entry<Direction, Station> entry : connections.entrySet()) {
            if (entry.getValue() == other) {
                dirToRemove = entry.getKey();
                break;
            }
        }

        if (dirToRemove != null) {
            connections.remove(dirToRemove);
            other.connections.remove(dirToRemove.getOpposite());

            // Update types for both stations
            this.updateType();
            other.updateType();
        }
    }

    /**
     * Automatically determines and updates station type based on connections
     */
    public void updateType() {
        int connectionCount = connections.size();

        // Проверяем только ортогональных соседей
        boolean hasAdjacentSameColor = false;
        boolean hasAdjacentDifferentColor = false;

        for (Direction dir : Direction.getOrthogonalDirections()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();
            Station neighbor = getWorld().getStationAt(nx, ny);
            if (neighbor != null && neighbor != this) {
                if (neighbor.getColor().equals(this.color)) {
                    hasAdjacentSameColor = true;
                } else {
                    hasAdjacentDifferentColor = true;
                }
            }
        }

        // Определяем новый тип
        if (hasAdjacentDifferentColor) {
            type = StationType.TRANSFER;
        } else if (hasAdjacentSameColor) {
            type = StationType.REGULAR; // Станции одного цвета остаются обычными
        } else if (connectionCount == 0) {
            type = StationType.REGULAR;
        } else if (connectionCount == 1) {
            type = StationType.TERMINAL;
        } else if (connectionCount == 2) {
            type = StationType.TRANSIT;
        }
    }
    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        if (getWorld().isRoundStationsEnabled()) {
            drawRoundStyle(g, offsetX, offsetY, zoom);
        } else {
            drawSquareStyle(g, offsetX, offsetY, zoom);
        }
    }
    private void drawRoundStyle(Graphics g, int offsetX, int offsetY, float zoom) {
        // Размер круга станции (меньше размера клетки)
        int drawSize = (int)(24 * zoom); // например, 8 пикселей при zoom=1

        int cellCenterX = (int)((getX() * 32 + offsetX +15) * zoom);
        int cellCenterY = (int)((getY() * 32 + offsetY +15) * zoom);
        // Позиция для рисования круга (чтобы он был по центру)
        int drawX = cellCenterX - drawSize/2;
        int drawY = cellCenterY - drawSize/2;
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Проверяем соседей
        Map<Direction, Station> adjacentStations = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.getOrthogonalDirections()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();
            Station neighbor = getWorld().getStationAt(nx, ny);
            if (neighbor != null && neighbor != this) {
                adjacentStations.put(dir, neighbor);
            }
        }

        if (type == StationType.PLANNED) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);
        } else if (type == StationType.BUILDING) {
            g2d.setColor(color);
            float[] dashPattern = {4 * zoom, 4 * zoom};
            g2d.setStroke(new BasicStroke(2 * zoom, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10, dashPattern, 0));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);
            g2d.setStroke(new BasicStroke(1 * zoom));
        } else {
            // Обычная отрисовка
            int holeSize = (int)(30 * zoom);
            int holeX = drawX + (drawSize - holeSize)/2;
            int holeY = drawY + (drawSize - holeSize)/2;

            g2d.setColor(getWorld().getWorldColorAt(getX(), getY()));
            g2d.fillOval(holeX, holeY, holeSize, holeSize);
            g2d.setColor(color);
            g2d.fillOval(drawX, drawY, drawSize, drawSize);
        }

        // Отрисовываем соединения с соседними станциями
        if (!adjacentStations.isEmpty()) {
            int framePadding = (int)(3 * zoom);
            int frameSize = drawSize + 2 * framePadding;
            int frameX = drawX - framePadding;
            int frameY = drawY - framePadding;

            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.setColor(Color.WHITE);

            // Рисуем внешнюю рамку
            g2d.drawOval(frameX, frameY, frameSize, frameSize);

            // Рисуем соединительные "перешейки"
            for (Map.Entry<Direction, Station> entry : adjacentStations.entrySet()) {
                Direction dir = entry.getKey();
                Station neighbor = entry.getValue();

                if (neighbor.getColor().equals(this.color)) {
                    drawRoundSameColorConnection(g2d, drawX, drawY, drawSize, frameX, frameY, frameSize, dir, zoom);
                } else {
                    drawRoundDifferentColorConnection(g2d, drawX, drawY, drawSize, frameX, frameY, frameSize, dir, zoom);
                }
            }
        }

        // Индикатор выделения
        if (selected) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4);
        }
    }

    private void drawRoundSameColorConnection(Graphics2D g, int drawX, int drawY, int drawSize,
            int frameX, int frameY, int frameSize, Direction dir, float zoom) {
        int connectorWidth = (int)(4 * zoom);
        int connectorLength = (int)(6 * zoom);

        switch (dir) {
            case NORTH:
                g.fillRect(drawX + drawSize/2 - connectorWidth/2, frameY - connectorLength,
                        connectorWidth, connectorLength);
                break;
            case SOUTH:
                g.fillRect(drawX + drawSize/2 - connectorWidth/2, frameY + frameSize,
                        connectorWidth, connectorLength);
                break;
            case EAST:
                g.fillRect(frameX + frameSize, drawY + drawSize/2 - connectorWidth/2,
                        connectorLength, connectorWidth);
                break;
            case WEST:
                g.fillRect(frameX - connectorLength, drawY + drawSize/2 - connectorWidth/2,
                        connectorLength, connectorWidth);
                break;
        }
    }

    private void drawRoundDifferentColorConnection(Graphics2D g, int drawX, int drawY, int drawSize,
            int frameX, int frameY, int frameSize, Direction dir, float zoom) {
        int connectorSize = (int)(8 * zoom);

        switch (dir) {
            case NORTH:
                g.fillOval(drawX + drawSize/2 - connectorSize/2, frameY - connectorSize/2,
                        connectorSize, connectorSize);
                break;
            case SOUTH:
                g.fillOval(drawX + drawSize/2 - connectorSize/2, frameY + frameSize - connectorSize/2,
                        connectorSize, connectorSize);
                break;
            case EAST:
                g.fillOval(frameX + frameSize - connectorSize/2, drawY + drawSize/2 - connectorSize/2,
                        connectorSize, connectorSize);
                break;
            case WEST:
                g.fillOval(frameX - connectorSize/2, drawY + drawSize/2 - connectorSize/2,
                        connectorSize, connectorSize);
                break;
        }
    }
    public void drawSquareStyle(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(20 * zoom);
        int drawX = (int)((getX() * 32 + offsetX +5) * zoom);
        int drawY = (int)((getY() * 32 + offsetY +5) * zoom);
        int arcSize = (int)(drawSize * 0.35);

        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Проверяем соседей
        Map<Direction, Station> adjacentStations = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.getOrthogonalDirections()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();
            Station neighbor = getWorld().getStationAt(nx, ny);
            if (neighbor != null && neighbor != this) {
                adjacentStations.put(dir, neighbor);
            }
        }

        if (type == StationType.PLANNED) {
            // Планируемая станция - только контур
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
        } else if (type == StationType.BUILDING) {
            // Строящаяся станция - пунктирный контур
            g2d.setColor(color);
            float[] dashPattern = {4 * zoom, 4 * zoom};
            g2d.setStroke(new BasicStroke(2 * zoom, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10, dashPattern, 0));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
            g2d.setStroke(new BasicStroke(1 * zoom));
        } else {
            // Обычная отрисовка для других типов
            g2d.setColor(color);
            g2d.fillRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
        }

        // Отрисовываем соединения между рамками
        if (!adjacentStations.isEmpty()) {
            int framePadding = (int)(2 * zoom);
            int frameSize = drawSize + 2 * framePadding;
            int frameX = drawX - framePadding;
            int frameY = drawY - framePadding;

            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.setColor(Color.WHITE);

            // Рисуем основную рамку
            g2d.drawRoundRect(frameX, frameY, frameSize, frameSize, arcSize, arcSize);

            // Рисуем соединительные "перешейки" к соседним станциям
            for (Map.Entry<Direction, Station> entry : adjacentStations.entrySet()) {
                Direction dir = entry.getKey();
                Station neighbor = entry.getValue();

                if (neighbor.getColor().equals(this.color)) {
                    // Соединение для станций одного цвета
                    drawSameColorConnection(g2d, drawX, drawY, drawSize, frameX, frameY, frameSize, dir, zoom);
                } else {
                    // Соединение для станций разного цвета
                    drawDifferentColorConnection(g2d, drawX, drawY, drawSize, frameX, frameY, frameSize, dir, zoom);
                }
            }
        }

        // Индикатор выделения
        if (selected) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4, arcSize, arcSize);
        }
    }

    private void drawSameColorConnection(Graphics2D g, int drawX, int drawY, int drawSize,
            int frameX, int frameY, int frameSize, Direction dir, float zoom) {
        int connectorWidth = (int)(4 * zoom);
        int connectorLength = (int)(6 * zoom);

        switch (dir) {
            case NORTH:
                g.fillRect(drawX + drawSize/2 - connectorWidth/2, frameY - connectorLength,
                        connectorWidth, connectorLength);
                break;
            case SOUTH:
                g.fillRect(drawX + drawSize/2 - connectorWidth/2, frameY + frameSize,
                        connectorWidth, connectorLength);
                break;
            case EAST:
                g.fillRect(frameX + frameSize, drawY + drawSize/2 - connectorWidth/2,
                        connectorLength, connectorWidth);
                break;
            case WEST:
                g.fillRect(frameX - connectorLength, drawY + drawSize/2 - connectorWidth/2,
                        connectorLength, connectorWidth);
                break;
        }
    }

    private void drawDifferentColorConnection(Graphics2D g, int drawX, int drawY, int drawSize,
            int frameX, int frameY, int frameSize, Direction dir, float zoom) {
        int connectorSize = (int)(8 * zoom);

        switch (dir) {
            case NORTH:
                g.fillRoundRect(drawX + drawSize/2 - connectorSize/2, frameY - connectorSize/2,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
            case SOUTH:
                g.fillRoundRect(drawX + drawSize/2 - connectorSize/2, frameY + frameSize - connectorSize/2,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
            case EAST:
                g.fillRoundRect(frameX + frameSize - connectorSize/2, drawY + drawSize/2 - connectorSize/2,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
            case WEST:
                g.fillRoundRect(frameX - connectorSize/2, drawY + drawSize/2 - connectorSize/2,
                        connectorSize, connectorSize, connectorSize, connectorSize);
                break;
        }
    }



    /**
     * Gets the direction from this station to another
     * @param other Target station
     * @return Direction to the other station
     */
    private Direction getDirectionTo(Station other) {
        int dx = other.getX() - x;
        int dy = other.getY() - y;

        if (dx == 0 && dy < 0) return Direction.NORTH;
        if (dx > 0 && dy < 0) return Direction.NORTHEAST;
        if (dx > 0 && dy == 0) return Direction.EAST;
        if (dx > 0 && dy > 0) return Direction.SOUTHEAST;
        if (dx == 0 && dy > 0) return Direction.SOUTH;
        if (dx < 0 && dy > 0) return Direction.SOUTHWEST;
        if (dx < 0 && dy == 0) return Direction.WEST;
        if (dx < 0 && dy < 0) return Direction.NORTHWEST;

        return Direction.NORTH; // default
    }
    public static final Color[] COLORS_DARK = {
            // Приглушенные, землистые тона
            new Color(160, 60, 50),    // Терракотовый
            new Color(80, 110, 60),     // Оливковый
            new Color(60, 100, 130),    // Приглушенный синий
            new Color(70, 80, 160),     // Сдержанный синий
            new Color(80, 140, 160),    // Морской
            new Color(120, 80, 50),     // Коричневый
            new Color(170, 100, 50),    // Тыквенный
            new Color(130, 70, 130),    // Приглушенный фиолетовый
            new Color(160, 140, 50),    // Горчичный
            new Color(140, 140, 140),   // Серый
            new Color(120, 150, 70),    // Оливково-зеленый
            new Color(70, 120, 120),    // Бирюзовый
            new Color(150, 80, 110),    // Приглушенный розовый
            new Color(50, 110, 90),     // Изумрудный
            new Color(150, 70, 80),     // Вишневый
            new Color(100, 130, 100)    // Мятный
    };
}



