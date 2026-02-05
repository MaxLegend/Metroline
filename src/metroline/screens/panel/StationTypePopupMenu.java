package metroline.screens.panel;

import metroline.objects.enums.StationType;
import metroline.objects.gameobjects.Station;
import metroline.screens.render.StationRender;
import metroline.util.ui.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StationTypePopupMenu extends JWindow {

    private final Station station;
    private JPanel contentPanel;
    private static final java.util.Map<Object, StationTypePopupMenu> openWindows = new java.util.HashMap<>();
    private static final int ICON_SIZE = 24;
    private AWTEventListener outsideClickListener; // Для отслеживания кликов вне меню

    public StationTypePopupMenu(Station station, int screenX, int screenY) {
        super();

        this.station = station;

        // Закрыть существующее окно для этой станции
        if (openWindows.containsKey(station)) {
            openWindows.get(station).dispose();
        }
        openWindows.put(station, this);

        initUI();
        positionWindow(screenX, screenY);
        addAutoCloseListener();
    }

    private void initUI() {
        setBackground(new Color(0, 0, 0, 0));

        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 30, 240));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.setColor(new Color(80, 80, 80, 150));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2d.dispose();
            }
        };
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPanel.setOpaque(false);

        // Панель элементов
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);

        // Добавляем кнопки для каждого типа станции (кроме авто-определяемых)
        for (StationType type : StationType.values()) {
            if (type == StationType.TRANSFER ||
                    type == StationType.TERMINAL ||
                    type == StationType.TRANSIT) {
                continue;
            }

            JButton typeButton = createTypeButton(type);
            itemsPanel.add(typeButton);
        }

        contentPanel.add(itemsPanel, BorderLayout.CENTER);
        setContentPane(contentPanel);
        pack();
    }

    private JButton createTypeButton(StationType type) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    g2d.setColor(new Color(60, 60, 60, 180));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        button.setLayout(new BorderLayout(8, 0));
        button.setPreferredSize(new Dimension(200, 35));
        button.setMaximumSize(new Dimension(200, 35));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Иконка
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                Station tempStation = new Station(station.getWorld(), 0, 0, station.getStationColor(), type);
                float zoom = 0.75f;
                int offsetX = (int) (ICON_SIZE / 2 / zoom - 16);
                int offsetY = (int) (ICON_SIZE / 2 / zoom - 16);

                StationRender.drawRoundStation(tempStation, g2d, offsetX, offsetY, zoom);

                g2d.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconPanel.setOpaque(false);
        button.add(iconPanel, BorderLayout.WEST);

        // Текст
        JLabel textLabel = new JLabel(type.getLocalizedName());
        textLabel.setFont(StyleUtil.getMetrolineFont(12));
        textLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        button.add(textLabel, BorderLayout.CENTER);

        // Действие
        button.addActionListener(e -> {
            station.setType(type);
            if (type == StationType.REGULAR) {
                station.autoDetectTypeFromRegular();
            }
            dispose();
        });

        return button;
    }

    private void positionWindow(int screenX, int screenY) {
        Rectangle screenBounds = getGraphicsConfiguration().getBounds();
        int windowWidth = getPreferredSize().width;
        int windowHeight = getPreferredSize().height;

        int finalX = screenX + 10;
        int finalY = screenY + 10;

        if (finalX + windowWidth > screenBounds.x + screenBounds.width) {
            finalX = screenBounds.x + screenBounds.width - windowWidth - 10;
        }
        if (finalY + windowHeight > screenBounds.y + screenBounds.height) {
            finalY = screenBounds.y + screenBounds.height - windowHeight - 10;
        }

        setLocation(finalX, finalY);
    }

    private void addAutoCloseListener() {
        // Закрытие по клавише Escape
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // Закрытие при клике ВНЕ границ меню (включая клики внутри главного окна приложения)
        outsideClickListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                    MouseEvent me = (MouseEvent) event;
                    Point clickPoint = me.getLocationOnScreen();
                    Rectangle popupBounds = new Rectangle(getLocationOnScreen(), getSize());

                    // Закрываем только если клик произошел ВНЕ прямоугольника меню
                    if (!popupBounds.contains(clickPoint)) {
                        SwingUtilities.invokeLater(() -> {
                            // Дополнительная проверка видимости для избежания гонок
                            if (isVisible()) {
                                dispose();
                            }
                        });
                    }
                }
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(
                outsideClickListener,
                AWTEvent.MOUSE_EVENT_MASK
        );
    }

    @Override
    public void dispose() {
        // Удаляем глобальный слушатель для предотвращения утечек памяти
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }

        openWindows.remove(station);
        super.dispose();
    }
}