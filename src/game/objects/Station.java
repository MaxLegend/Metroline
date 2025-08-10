package game.objects;

import game.core.GameObject;
import game.core.world.GameWorld;
import game.core.world.World;
import game.objects.enums.Direction;
import game.objects.enums.StationType;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;


/**
 * Station game object
 */
public class Station extends GameObject {


    private String name;



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



    private String generateRandomName() {
        Random rand = new Random();
        return GameConstants.NAME_PARTS[rand.nextInt(GameConstants.NAME_PARTS.length)];
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
     * @param newType New station type
     */
    public void setType(StationType newType) {
        // Запрещаем недопустимые переходы между типами
        if (this.type == StationType.PLANNED && newType != StationType.BUILDING) {
    //        MetroLogger.logWarning("Attempt to change PLANNED station to " + newType +" - only BUILDING is allowed");
            return;
        }
        if (newType == StationType.CLOSED &&
                (this.type == StationType.PLANNED || this.type == StationType.BUILDING)) {
     //       MetroLogger.logWarning("Cannot close PLANNED or BUILDING station");
            return;
        }
        if (this.type == StationType.BUILDING && newType == StationType.PLANNED) {
     //       MetroLogger.logWarning("Attempt to change BUILDING station back to PLANNED");
            return;
        }

        // Для TRANSFER станций проверяем соседей
        if (newType == StationType.TRANSFER) {
            boolean hasDifferentColorNeighbor = false;
            for (Direction dir : Direction.getOrthogonalDirections()) {
                int nx = x + dir.getDx();
                int ny = y + dir.getDy();
                Station neighbor = getWorld().getStationAt(nx, ny);
                if (neighbor != null && !neighbor.getColor().equals(this.color)) {
                    hasDifferentColorNeighbor = true;
                    break;
                }
            }

            if (!hasDifferentColorNeighbor) {
            //    MetroLogger.logWarning("Attempt to set TRANSFER type without different color neighbors");
                return;
            }
        }

       // MetroLogger.logInfo("Changing station " + getName() + " type from " + this.type + " to " + newType);

        this.type = newType;

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
        // Сохраняем особые типы
//        if (this.type == StationType.PLANNED || this.type == StationType.BUILDING) {
//            return;
//        }

        // Проверяем соседей
        boolean hasSameColorNeighbor = false;
        boolean hasDifferentColorNeighbor = false;

        for (Direction dir : Direction.values()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();
            Station neighbor = getWorld().getStationAt(nx, ny);
            if (neighbor != null) {
                if (neighbor.getColor().equals(this.color)) {
                    hasSameColorNeighbor = true;
                } else {
                    hasDifferentColorNeighbor = true;
                }
            }
        }
        StationType newType = StationType.REGULAR;
 

        if (hasDifferentColorNeighbor) {
            newType = StationType.TRANSFER;
        } else if (connections.size() == 0) {
            newType = StationType.REGULAR;
        } else if (connections.size() == 1 ) {
            newType = StationType.TERMINAL;
        } else if (connections.size() == 2 ) {
            newType = StationType.TRANSIT;
        }
        if(this.type != StationType.PLANNED) {
            if (this.type != newType) {

         //   MetroLogger.logInfo("Auto-updating station " + getName() + " type from " + this.type + " to " + newType);
            this.type = newType;
            }
        }
    }
//
//    @Override
//    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
//        if (getWorld().isRoundStationsEnabled()) {
//            drawRoundStyle(g, offsetX, offsetY, zoom);
//        } else {
//            drawSquareStyle(g, offsetX, offsetY, zoom);
//        }
//    }
@Override
public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
    if (getWorld().isRoundStationsEnabled()) {
        drawWorldColorRing(g, offsetX, offsetY, zoom);
        drawRoundTransfer(g, offsetX, offsetY, zoom);
        drawRoundStation(g, offsetX, offsetY, zoom);
        if(selected) drawRoundSelection(g, offsetX, offsetY, zoom);
    } else {
        drawWorldColorSquare(g, offsetX, offsetY, zoom);
        drawRoundTransfer(g, offsetX, offsetY, zoom);
        drawSquareStation(g, offsetX, offsetY, zoom);
        if(selected) drawSquareSelection(g, offsetX, offsetY, zoom);
    }

}

    public void drawWorldColorRing(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (24 * zoom);
        int cellCenterX = (int) ((getX() * 32 + offsetX + 15) * zoom);
        int cellCenterY = (int) ((getY() * 32 + offsetY + 15) * zoom);
        int drawX = cellCenterX - drawSize / 2;
        int drawY = cellCenterY - drawSize / 2;
        int holeSize = (int) (30 * zoom);
        int holeX = drawX + (drawSize - holeSize) / 2;
        int holeY = drawY + (drawSize - holeSize) / 2;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(getWorld().getWorldColorAt(getX(), getY()));
        g2d.fillOval(holeX, holeY, holeSize, holeSize);
    }
    public void drawRoundSelection(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (24 * zoom);
        int cellCenterX = (int) ((getX() * 32 + offsetX + 15) * zoom);
        int cellCenterY = (int) ((getY() * 32 + offsetY + 15) * zoom);
        int drawX = cellCenterX - drawSize / 2;
        int drawY = cellCenterY - drawSize / 2;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.drawOval(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4);
    }

    public void drawRoundTransfer(Graphics g, int offsetX, int offsetY, float zoom) {
        int cellCenterX = (int) ((getX() * 32 + offsetX + 15) * zoom);
        int cellCenterY = (int) ((getY() * 32 + offsetY + 15) * zoom);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Map<Direction, Station> adjacentStations = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();
            Station neighbor = getWorld().getStationAt(nx, ny);
            if (neighbor != null && neighbor != this) {
                adjacentStations.put(dir, neighbor);
            }
        }
        if (type == StationType.TRANSFER && !adjacentStations.isEmpty()) {
            float connectionWidth = 14.0f * zoom; // Утолщенные соединения

            for (Map.Entry<Direction, Station> entry : adjacentStations.entrySet()) {
                Direction dir = entry.getKey();
                Station neighbor = entry.getValue();

                if (!neighbor.getColor().equals(this.color)) {
                    int nx = neighbor.getX() * 32 + offsetX + 15;
                    int ny = neighbor.getY() * 32 + offsetY + 15;

                    drawTransferConnection(g2d, cellCenterX, cellCenterY, nx, ny, dir, zoom);

                    // Градиентное соединение
                    Color[] colors = {
                            color,                // Начальный цвет (100%)
                            color,                // Сохраняет цвет до 40%
                            neighbor.getColor(),  // Резкий переход
                            neighbor.getColor()   // Конечный цвет (100%)
                    };
                    float[] fractions = {0f, 0.35f, 0.65f, 1f};

                    LinearGradientPaint gradient = new LinearGradientPaint(
                            new Point2D.Float(cellCenterX, cellCenterY),
                            new Point2D.Float(nx, ny),
                            fractions,
                            colors
                    );
                    g2d.setPaint(gradient);
                    g2d.setStroke(new BasicStroke(connectionWidth,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    drawTransferConnection(g2d, cellCenterX, cellCenterY, nx, ny, dir, zoom);
                }
            }
        }
    }
    public void drawRoundStation(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (24 * zoom);
        int cellCenterX = (int) ((getX() * 32 + offsetX + 15) * zoom);
        int cellCenterY = (int) ((getY() * 32 + offsetY + 15) * zoom);
        int drawX = cellCenterX - drawSize / 2;
        int drawY = cellCenterY - drawSize / 2;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        } else if (type == StationType.DESTROYED) {
            // Разрушаемая станция - контур с крестом
            int crossPadding = (int) (6 * zoom);

            // Основной контур
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);

            // Крест внутри
            g2d.setStroke(new BasicStroke(3 * zoom));
            // Диагональ 1
            g2d.setColor(color.darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
            // Диагональ 2
            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        }
        else if (type == StationType.CLOSED) {
            // Закрытая станция - контур с диагональной линией
            int crossPadding = (int) (6 * zoom);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawOval(drawX, drawY, drawSize, drawSize);

            // Диагональная линия
            g2d.setColor(color.darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
        } else {
            g2d.setColor(color);
            g2d.fillOval(drawX, drawY, drawSize, drawSize);
        }

    }

    private void drawTransferConnection(Graphics2D g2d, int x1, int y1, int x2, int y2,
            Direction dir, float zoom) {
        Path2D path = new Path2D.Float();
        path.moveTo(x1, y1);

        int ctrlX = (x1 + x2) / 2;
        int ctrlY = (y1 + y2) / 2;
        int curveIntensity = (int)(12 * zoom);

        switch (dir) {
            case NORTH:
                ctrlY -= curveIntensity;
                break;
            case NORTHEAST:
                ctrlX += curveIntensity;
                ctrlY -= curveIntensity;
                break;
            case EAST:
                ctrlX += curveIntensity;
                break;
            case SOUTHEAST:
                ctrlX += curveIntensity;
                ctrlY += curveIntensity;
                break;
            case SOUTH:
                ctrlY += curveIntensity;
                break;
            case SOUTHWEST:
                ctrlX -= curveIntensity;
                ctrlY += curveIntensity;
                break;
            case WEST:
                ctrlX -= curveIntensity;
                break;
            case NORTHWEST:
                ctrlX -= curveIntensity;
                ctrlY -= curveIntensity;
                break;
        }

        path.quadTo(ctrlX, ctrlY, x2, y2);
        g2d.draw(path);
    }
    public void drawWorldColorSquare(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(20 * zoom);
        int drawX = (int)((getX() * 32 + offsetX +5) * zoom);
        int drawY = (int)((getY() * 32 + offsetY +5) * zoom);
        int arcSize = (int)(drawSize * 0.35);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(getWorld().getWorldColorAt(getX(), getY()));
        g2d.fillRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
    }
    public void drawSquareSelection(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(20 * zoom);
        int drawX = (int)((getX() * 32 + offsetX +5) * zoom);
        int drawY = (int)((getY() * 32 + offsetY +5) * zoom);
        int arcSize = (int)(drawSize * 0.35);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.drawRoundRect(drawX - 2, drawY - 2, drawSize + 4, drawSize + 4, arcSize, arcSize);
    }
    public void drawSquareStation(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(20 * zoom);
        int drawX = (int)((getX() * 32 + offsetX +5) * zoom);
        int drawY = (int)((getY() * 32 + offsetY +5) * zoom);
        int arcSize = (int)(drawSize * 0.35);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        } else if (type == StationType.DESTROYED) {
            // Разрушаемая станция - контур с крестом
            int crossPadding = (int)(4 * zoom);

            // Основной контур
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            // Крест внутри
            g2d.setStroke(new BasicStroke(3 * zoom));
            // Диагональ 1
            g2d.setColor(color.darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
            // Диагональ 2
            g2d.drawLine(drawX + drawSize - crossPadding,
                    drawY + crossPadding,
                    drawX + crossPadding,
                    drawY + drawSize - crossPadding);
        } else if (type == StationType.CLOSED) {
            // Закрытая станция - контур с диагональной линией
            int crossPadding = (int)(4 * zoom);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);

            // Диагональная линия
            g2d.setColor(color.darker().darker());
            g2d.drawLine(drawX + crossPadding,
                    drawY + crossPadding,
                    drawX + drawSize - crossPadding,
                    drawY + drawSize - crossPadding);
        } else {
            // Обычная отрисовка для других типов
            g2d.setColor(color);
            g2d.fillRoundRect(drawX, drawY, drawSize, drawSize, arcSize, arcSize);
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

}



