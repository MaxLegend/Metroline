package metroline.input;

import metroline.objects.gameobjects.GameObject;
import metroline.core.time.GameTime;
import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.*;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.Label;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.util.MetroLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import static metroline.screens.worldscreens.WorldGameScreen.getInstance;

public class GameClickHandler {
    public static Station selectedStation = null;
    public GameObject selectedObject = null;
    static boolean dragging = false;
    static PathPoint dragOffset = null;
    public WorldGameScreen screen;
    private static Color currentStationColor = GameConstants.COLORS[0]; // Красный по умолчанию
    boolean colorSelectionEnabled = false;



    public static final int STATION_BASE_COST = 100;
    public static final int TUNNEL_COST_PER_SEGMENT = 10;


    public GameClickHandler(WorldGameScreen screen) {
        this.screen = screen;
    }

    /*********************************
     * CLICK HANDLER METHODS SECTION
     *********************************/

    public void mainClickHandler(int x, int y) {
        if (screen.isAltPressed) {
            this.handleAltClick(x, y);
        }
        else
        if (screen.isShiftPressed && screen.isCPressed) {
            this.showColorSelectionPopup(x, y);
        }
        else if (screen.isShiftPressed && !screen.isCtrlPressed) {
            this.handleShiftClick(x, y);
        }
        else if (screen.isCtrlPressed) {
            this.handleCtrlClick(x, y);
        }
        else {
            this.handleDefaultLeftClick(x, y);
        }
    }
    /**
     * Handles Alt+Click to switch between PLANNED and BUILDING states
     */
    public void handleAltClick(int x, int y) {
        GameWorld world = (GameWorld)WorldGameScreen.getInstance().getWorld();
        Station station = world.getStationAt(x, y);

        if (station != null && station.getType() == StationType.PLANNED) {
            int stationCost = calculateStationCost(station);
            if (world.canAfford(stationCost)) {
                world.addMoney(-stationCost);
                station.setType(StationType.BUILDING);
                world.addStation(station);
            }
        }
        WorldGameScreen.getInstance().repaint();
    }
    public void handleCtrlDoubleLeftClick(int x, int y) {
    //   System.out.println("isAPressed " + screen.isAPressed);
            if (this.getSelectedObject() != null ) {
                screen.showInfoPanel(this.getSelectedObject(), x, y);
            } else {
                screen.infoPanel.hidePanel();
            }
    }

    /**
     * Handles mouse drag in edit mode
     * @param x X coordinate
     * @param y Y coordinate
     */
    public  void handleEditDrag(int x, int y) {
        if ( selectedObject != null && dragOffset != null) {
            // Проверяем границы
            if (x < 0 || x >= WorldGameScreen.getInstance().getWorld().getWidth() || y < 0 || y >= WorldGameScreen.getInstance().getWorld().getHeight()) {
                return;
            }

            if (selectedObject instanceof Label) {

                Label label = (Label) selectedObject;
                int newX = x - dragOffset.x;
                int newY = y - dragOffset.y;

                if (label.tryMoveTo(newX, newY)) {
                    WorldGameScreen.getInstance().repaint();
                }
                return;
            }

            if ( selectedObject instanceof Station) {
                Station station = (Station)selectedObject;
                if (station.getType() != StationType.PLANNED) return;
                int newX = x - dragOffset.x;
                int newY = y - dragOffset.y;

                // Check if new position is valid
                if (newX >= 0 && newX < WorldGameScreen.getInstance().getWorld().getWidth() &&
                        newY >= 0 && newY < WorldGameScreen.getInstance().getWorld().getHeight() &&
                        WorldGameScreen.getInstance().getWorld().getStationAt(newX, newY) == null) {
                    Label label = WorldGameScreen.getInstance().getWorld().getLabelForStation(station);
                    // Remove from old position
                    WorldGameScreen.getInstance().getWorld().getGameGrid()[station.getX()][station.getY()].setContent(null);

                    // Update position
                    station.x = newX;
                    station.y = newY;

                    // Add to new position
                    WorldGameScreen.getInstance().getWorld().getGameGrid()[newX][newY].setContent(station);

                    // Recalculate all connected tunnels
                    for (Tunnel t : WorldGameScreen.getInstance().getWorld().getTunnels()) {
                        if (t.getStart() == station || t.getEnd() == station) {
                            t.calculatePath();
                        }
                    }
                    if (label != null) {
                        // Находим новую позицию для метки (рядом со станцией)
                        PathPoint newLabelPos = WorldGameScreen.getInstance().getWorld().findFreePositionNear(station.getX(), station.getY(), station.getName());
                        if (newLabelPos != null) {
                            label.tryMoveTo(newLabelPos.x, newLabelPos.y);
                        }
                    }
                    WorldGameScreen.getInstance().repaint();
                }
            }
            else if (selectedObject instanceof Tunnel) {

                Tunnel tunnel = (Tunnel)selectedObject;
                if (tunnel.getType() != TunnelType.PLANNED) return;
                // Проверяем, что новая позиция не совпадает со станцией
                Station stationAtPos = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
                if (stationAtPos == null) {
                    tunnel.moveControlPoint(x, y);
                    WorldGameScreen.getInstance().repaint();
                }
            }
        }
    }

    public void handleEditStationName(int x, int y) {
        Station station = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
        Tunnel t = WorldGameScreen.getInstance().getWorld().getTunnelAt(x, y);
        if (station != null) {
            editStationName(station);
        }
    }
    /**
     * Handles tunnel creation
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleCtrlClick(int x, int y) {
        GameWorld world = (GameWorld)WorldGameScreen.getInstance().getWorld();
        Station station = world.getStationAt(x, y);
        if (station == null) return;

        // Если есть уже выбранная станция и это не та же самая

        if (selectedStation != null && selectedStation != station) {
            // Создаём туннель
            Tunnel tunnel = new Tunnel(world, selectedStation, station, TunnelType.PLANNED);

            world.addTunnel(tunnel);

            // Снимаем выделение
            selectedStation.setSelected(false);
            selectedStation = null;

        } else {
            // Выбираем новую станцию (или снимаем выделение если кликнули ту же)
            if (selectedStation == station) {
                station.setSelected(false);
                selectedStation = null;
            } else {
                deselectAll(); // Снимаем выделение со всех других объектов
                station.setSelected(true);
                selectedStation = station;
            }
        }

        WorldGameScreen.getInstance().repaint();
    }
    /**
     * Handles selection of stations/tunnels
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleDefaultLeftClick(int x, int y) {
        // Сначала снимаем выделение со всех объектов
        deselectAll();

        Label label = WorldGameScreen.getInstance().getWorld().getLabelAt(x, y);
        if (label != null) {
            label.setSelected(true);
            selectedObject = label;
            dragOffset = new PathPoint(x - label.getX(), y - label.getY());
            WorldGameScreen.getInstance().repaint();
            return;
        }

        // Проверяем станции
        Station station = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
        if (station != null) {
            station.setSelected(true);
            selectedObject = station;
            dragOffset = new PathPoint(x - station.getX(), y - station.getY());
            WorldGameScreen.getInstance().repaint();
            return;
        }

        // Проверяем туннели
        Tunnel tunnel = WorldGameScreen.getInstance().getWorld().getTunnelAt(x, y);
        if (tunnel != null) {
            // Ищем конкретную точку туннеля
            for (PathPoint p : tunnel.getPath()) {
                if (p.getX() == x && p.getY() == y) {
                    tunnel.setSelected(true);
                    selectedObject = tunnel;
                    dragOffset = new PathPoint(0, 0);
                    break;
                }
            }
            WorldGameScreen.getInstance().repaint();
            return;
        }

    }
    /**
     * Handles station placement/removal
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleShiftClick(int x, int y) {
        Station existing = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
        if (existing != null) {
            if (!existing.selected && !getInstance().isShiftPressed) {
                existing.setSelected(true);
            } else if(getInstance().isShiftPressed) {
                GameWorld gameWorld = (GameWorld) WorldGameScreen.getInstance().getWorld();
                if(getInstance().isAltPressed) {
                    gameWorld.startDestroyingStation(existing);
                    return;
                }
                if(existing.getType() == StationType.PLANNED) {
                    WorldGameScreen.getInstance().getWorld().removeStation(existing);
                }
                if(existing.getType() == StationType.CLOSED) {
                    existing.setType(StationType.REGULAR);
                } else {
                    existing.setType(StationType.CLOSED);
                }
                //     WorldGameScreen.getInstance().getWorld().removeStation(existing);
            }
            return; // Выходим, если станция уже существует
        }
        if (WorldGameScreen.getInstance().isCPressed) { // Проверяем зажат ли Ctrl+C
            showColorSelectionPopup(x, y);
            return; // Выходим, чтобы не создавать станцию дважды
        }

        // Создаем новую станцию с текущим цветом
        Station station = new Station(WorldGameScreen.getInstance().getWorld(),x, y, currentStationColor, StationType.PLANNED);
        WorldGameScreen.getInstance().getWorld().addStation(station);
        checkForTransferStation(station);
    }

    /*****************************
     * AUXILIARY METHODS SECTION
     ****************************/
    public void completeConstruction(Station station) {
        if (station.getType() == StationType.BUILDING) {
            station.updateType();
            if(WorldGameScreen.getInstance().getWorld() instanceof GameWorld world) world.updateConnectedTunnels(station);
            WorldGameScreen.getInstance().repaint();
        }
    }

    public void completeConstruction(Tunnel tunnel) {
        if (tunnel.getType() == TunnelType.BUILDING) {
            // Проверяем, что обе станции построены
            if (tunnel.getStart().getType() != StationType.PLANNED &&
                    tunnel.getStart().getType() != StationType.BUILDING &&
                    tunnel.getEnd().getType() != StationType.PLANNED &&
                    tunnel.getEnd().getType() != StationType.BUILDING) {
                tunnel.setType(TunnelType.ACTIVE);
                WorldGameScreen.getInstance().repaint();
            }
        }
    }

    public void showColorSelectionPopup(int x, int y) {
        // Создаем прозрачное безрамочное окно
        Window parentWindow = SwingUtilities.getWindowAncestor(WorldGameScreen.getInstance());
        JDialog colorDialog = new JDialog(parentWindow);
        colorDialog.setUndecorated(true); // Убираем рамку и заголовок
        colorDialog.setBackground(new Color(0, 0, 0, 0)); // Прозрачный фон

        JPanel colorPanel = new JPanel(new GridLayout(2, 2));
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2)); // Белая рамка
        colorPanel.setOpaque(true);
        colorPanel.setBackground(new Color(50, 50, 50)); // Темный фон

        // Конвертируем мировые координаты в экранные
        Point screenPos = WorldGameScreen.getInstance().worldToScreen(x, y);

        for (Color color : GameConstants.COLORS) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(color);
            colorBtn.setPreferredSize(new Dimension(30, 30));
            colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Убираем стандартную рамку кнопки
            colorBtn.setContentAreaFilled(false); // Прозрачная область кнопки
            colorBtn.setOpaque(true); // Но сам цвет видимый
            colorBtn.setFocusPainted(false); // Убираем эффект фокуса

            colorBtn.addActionListener(e -> {
                GameClickHandler.currentStationColor = color;
                Station newStation = new Station(WorldGameScreen.getInstance().getWorld(),x, y, color, StationType.PLANNED);
                WorldGameScreen.getInstance().getWorld().addStation(newStation);
                WorldGameScreen.getInstance().repaint();
                colorDialog.dispose();
                colorSelectionEnabled = false;
            });

            // Эффект при наведении
            colorBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    colorBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                }
                public void mouseExited(MouseEvent e) {
                    colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                }
            });

            colorPanel.add(colorBtn);
        }

        colorDialog.add(colorPanel);
        colorDialog.pack();

        // Позиционируем окно рядом с курсором
        colorDialog.setLocation(
                screenPos.x + 20, // Смещение от курсора
                screenPos.y + 20
        );

        // Закрытие при клике вне окна
        colorDialog.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent e) {
                colorDialog.dispose();
                colorSelectionEnabled = false;
            }
            @Override
            public void windowClosed(WindowEvent e) {
                colorDialog.dispose();
                colorSelectionEnabled = false; // Сбрасываем флаг при закрытии
            }
        });

        colorDialog.setVisible(true);
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }

    /**
     * Deselects all game objects
     */
    private void deselectAll() {
        // Снимаем выделение со всех станций
        for (Station station : WorldGameScreen.getInstance().getWorld().getStations()) {
            station.setSelected(false);
        }

        // Снимаем выделение со всех меток
        for (Label label : WorldGameScreen.getInstance().getWorld().getLabels()) {
            label.setSelected(false);
        }

        // Снимаем выделение со всех туннелей
        for (Tunnel tunnel : WorldGameScreen.getInstance().getWorld().getTunnels()) {
            tunnel.setSelected(false);
        }

        selectedObject = null;
    }

    /**
     * Edit station name
     */
    static void editStationName(Station station) {
        // Создаем панель с текстовым полем
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Station Name:");
        JTextField textField = new JTextField(station.getName(), 20);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        // Показываем диалоговое окно
        int result = JOptionPane.showConfirmDialog(
                WorldGameScreen.getInstance(),
                panel,
                "Edit name",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Обрабатываем результат
        if (result == JOptionPane.OK_OPTION) {
            String newName = textField.getText().trim();
            if (!newName.isEmpty()) {
                station.setName(newName);
                WorldGameScreen.getInstance().repaint();
            }
        }
    }


    /**
     * Checks if a station should be converted to a transfer station
     * @param station Station to check
     */
    private static void checkForTransferStation(Station station) {
        int x = station.getX();
        int y = station.getY();
        GameTime gameTime = station.getWorld().getGameTime();
        // Check all 8 directions
        for (int ny = Math.max(0, y-1); ny < Math.min(WorldGameScreen.getInstance().getWorld().getHeight(), y+2); ny++) {
            for (int nx = Math.max(0, x-1); nx < Math.min(WorldGameScreen.getInstance().getWorld().getWidth(), x+2); nx++) {
                if (nx == x && ny == y) continue;

                Station neighbor = WorldGameScreen.getInstance().getWorld().getStationAt(nx, ny);
                if (neighbor != null && !neighbor.getColor().equals(station.getColor())) {
                    station.setType(StationType.TRANSFER);
                    neighbor.setType(StationType.TRANSFER);
                    return;
                }
            }
        }
    }
    public void checkConstructionProgress() {
        GameWorld world = (GameWorld)WorldGameScreen.getInstance().getWorld();

        // Создаем копии списков для безопасной итерации
        List<Station> stationsToCheck = new ArrayList<>(world.getStations());
        List<Tunnel> tunnelsToCheck = new ArrayList<>(world.getTunnels());

        // Для станций
        for (Station station : stationsToCheck) {
            try {
                if (station.getType() == StationType.BUILDING || station.getType() == StationType.DESTROYED) {
                    float progress = world.getStationConstructionProgress(station);
                    if (station.getType() == StationType.BUILDING && progress >= 1.0f) {
                        completeConstruction(station);
                    } else if (station.getType() == StationType.DESTROYED && progress <= 0f) {
                        world.removeStation(station);
                        WorldGameScreen.getInstance().repaint();
                    }
                }
            } catch (Exception e) {
                MetroLogger.logError("Error processing station " + station.getName(), e);
            }
        }

        // Для туннелей
        for (Tunnel tunnel : tunnelsToCheck) {
            try {
                if (tunnel.getType() == TunnelType.BUILDING || tunnel.getType() == TunnelType.DESTROYED) {
                    float progress = world.getTunnelConstructionProgress(tunnel);
                    if (tunnel.getType() == TunnelType.BUILDING && progress >= 1.0f) {
                        completeConstruction(tunnel);
                    } else if (tunnel.getType() == TunnelType.DESTROYED && progress <= 0f) {
                        world.removeTunnel(tunnel);
                        WorldGameScreen.getInstance().repaint();
                    }
                }
            } catch (Exception e) {
                MetroLogger.logError("Error processing tunnel", e);
            }
        }
    }
    private int calculateStationCost(Station station) {
        int totalCost = STATION_BASE_COST;

        // Добавляем стоимость всех связанных туннелей
        for (Tunnel tunnel : WorldGameScreen.getInstance().getWorld().getTunnels()) {
            if ((tunnel.getStart() == station || tunnel.getEnd() == station) &&
                    tunnel.getType() == TunnelType.PLANNED) {
                totalCost += tunnel.getLength() * TUNNEL_COST_PER_SEGMENT;
            }
        }

        return totalCost;
    }
}

