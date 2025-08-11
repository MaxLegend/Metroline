package metroline.screens.panel;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.objects.enums.StationColors;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.screens.worldscreens.WorldSandboxScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.util.LngUtil;
import metroline.util.StyleUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;



public class InfoWindow extends JWindow {
    private JLabel titleLabel;
    private JLabel infoLabel;
    private JProgressBar progressBar;
    private Timer updateTimer;
    public Object currentObject;
    private JPanel headerPanel;
    private Point dragStartPoint;
    private Point windowStartPoint;
    private JPanel contentPanel;
    private JButton closeButton;
    public InfoWindow(Window owner) {
        super(owner); // Создаем окно без владельца

        // Настройка прозрачности и формы окна
        setBackground(new Color(0, 0, 0, 0));

        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                // Рисуем темную подложку с закругленными углами
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 30, 240));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Рисуем тонкую рамку
                g2d.setColor(new Color(80, 80, 80, 150));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);

                g2d.dispose();
            }
        };

        contentPanel.setLayout(new BorderLayout(5, 5));
        contentPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        contentPanel.setOpaque(false);

        // Панель заголовка (теперь занимает всю высоту окна)
        headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Рисуем разделительную линию
                g.setColor(new Color(80, 80, 80, 150));
                g.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
            }
        };
        headerPanel.setOpaque(false);

        titleLabel = new JLabel("", SwingConstants.LEFT);
        titleLabel.setFont(StyleUtil.getMetrolineFont(13));
        titleLabel.setForeground(StyleUtil.FOREGROUND_COLOR);

        // Кнопка закрытия
        closeButton = new JButton("×");
        closeButton.setFont(StyleUtil.getMetrolineFont(14));
        closeButton.setForeground(StyleUtil.FOREGROUND_COLOR);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.addActionListener(e -> hideWindow());

        // Обработчики для перетаскивания за любую часть окна
        MouseAdapter dragAdapter = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getLocationOnScreen();
                windowStartPoint = getLocation();
            }

            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null && windowStartPoint != null) {
                    Point currentPoint = e.getLocationOnScreen();
                    int deltaX = currentPoint.x - dragStartPoint.x;
                    int deltaY = currentPoint.y - dragStartPoint.y;
                    setLocation(windowStartPoint.x + deltaX, windowStartPoint.y + deltaY);
                }
            }
        };

        // Добавляем обработчики перетаскивания ко всем компонентам
        contentPanel.addMouseListener(dragAdapter);
        contentPanel.addMouseMotionListener(dragAdapter);
        headerPanel.addMouseListener(dragAdapter);
        headerPanel.addMouseMotionListener(dragAdapter);

        // Основная информация
        infoLabel = new JLabel();
        infoLabel.setFont(StyleUtil.getMetrolineFont(13));
        infoLabel.setForeground(new Color(200, 200, 200));

        // Прогресс-бар
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(120, 14));
        progressBar.setForeground(new Color(79, 155, 155));
        progressBar.setBackground(new Color(60, 60, 60));
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setBorder(BorderFactory.createEmptyBorder());

        // Собираем заголовок
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(closeButton, BorderLayout.EAST);

        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(infoLabel, BorderLayout.CENTER);
        contentPanel.add(progressBar, BorderLayout.SOUTH);

        setContentPane(contentPanel);

        // Таймер для обновления информации
        updateTimer = new Timer(200, e -> updateInfo());
        updateTimer.start();

        // Установка начального размера
        pack();
    }
    public static void updateWindowsVisibility(MainFrame frame) {
        boolean shouldShow = frame.getCurrentScreen() instanceof WorldScreen ;
        for (Window window : frame.getOwnedWindows()) {
            if (window instanceof InfoWindow) {
                window.setVisible(shouldShow);
            }
        }
    }
    public void displayStationInfo(Station station, Point location) {
        this.currentObject = station;
        updateInfo();
        setLocation(location);
        setVisible(true);
        pack(); // Обновляем размер окна под содержимое
    }

    public void displayTunnelInfo(Tunnel tunnel, Point location) {
        this.currentObject = tunnel;
        updateInfo();
        setLocation(location);
        setVisible(true);
        pack(); // Обновляем размер окна под содержимое
    }

    public void updateInfo() {
        if (currentObject instanceof Station) {
            Station station = (Station) currentObject;
            titleLabel.setText(station.getName());

            StringBuilder info = new StringBuilder("<html>");
            info.append(LngUtil.translatable("infoWnd.position") + " ").append(station.getX()).append(", ").append(station.getY()).append("<br>");
            info.append(LngUtil.translatable("infoWnd.type") + " ").append(station.getType().getLocalizedName()).append("<br>");
            info.append(LngUtil.translatable("infoWnd.color") + " ").append(station.getStationColor().getLocalizedName()).append("<br>");
            info.append(LngUtil.translatable("infoWnd.cost") + " ").append(NumberFormat.getIntegerInstance().format(50000)).append(" ₽").append("<br>");



            infoLabel.setText(info.toString());
            updateProgress();
        } else if (currentObject instanceof Tunnel) {
            Tunnel tunnel = (Tunnel) currentObject;
            titleLabel.setText(LngUtil.translatable("infoWnd.tunnel_title"));

            StringBuilder info = new StringBuilder("<html>");
            info.append(LngUtil.translatable("infoWnd.tunnel_from")+ " ").append(tunnel.getStart().getName()).append("<br>");
            info.append(LngUtil.translatable("infoWnd.tunnel_to")+ " ").append(tunnel.getEnd().getName()).append("<br>");
            info.append(LngUtil.translatable("infoWnd.tunnel_length")+ " ").append(tunnel.getLength()).append( " " + LngUtil.translatable("infoWnd.tunnel_segments") + " <br>");
            info.append(LngUtil.translatable("infoWnd.tunnel_type")+ " ").append(tunnel.getType().getLocalizedName()+ "<br>");
            int cost = tunnel.getLength() * 10000;
            info.append(LngUtil.translatable("infoWnd.tunnel_cost")+ " ").append(NumberFormat.getIntegerInstance().format(cost)).append(" ₽" + " <br>");


            infoLabel.setText(info.toString());
            updateProgress();
        }

        pack(); // Подгоняем размер окна под содержимое
    }

    private void updateProgress() {
        if (currentObject instanceof Station) {
            Station station = (Station) currentObject;
            float progress = ((GameWorld) WorldGameScreen.getInstance().getWorld()).getStationConstructionProgress(station);
            if (progress > 0 && progress < 1) {
                progressBar.setVisible(true);
                progressBar.setValue((int)(progress * 100));
                progressBar.setString((int)(progress * 100) + "%");
            } else {
                progressBar.setVisible(false);
            }
        } else if (currentObject instanceof Tunnel) {
            Tunnel tunnel = (Tunnel) currentObject;
            float progress = ((GameWorld)WorldGameScreen.getInstance().getWorld()).getTunnelConstructionProgress(tunnel);
            if (progress > 0 && progress < 1) {
                progressBar.setVisible(true);
                progressBar.setValue((int)(progress * 100));
                progressBar.setString((int)(progress * 100) + "%");
            } else {
                progressBar.setVisible(false);
            }
        }
    }

    public void hideWindow() {
        currentObject = null;
        setVisible(false);
    }

    @Override
    public void setVisible(boolean visible) {

            if (visible) {
                setAlwaysOnTop(true);
                updateInfo();
                pack();
            }
            super.setVisible(visible && getOwner().isVisible());
        }
    }
