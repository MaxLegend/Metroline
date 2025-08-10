package metroline.screens.panel;

import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;

public class InfoPanel extends JPanel {
    private JLabel titleLabel;
    private JLabel infoLabel;
    private JProgressBar progressBar;
    private Timer updateTimer;
    public Object currentObject;
    private JPanel headerPanel;
    private JButton closeButton;
    private Point dragStartPoint;
    private Point panelStartPoint;

    public InfoPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // Основная панель с закругленными углами и тенью
        JPanel contentPanel = new JPanel() {
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
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        contentPanel.setOpaque(false);

        // Панель заголовка с кнопкой закрытия
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        titleLabel = new JLabel("", SwingConstants.LEFT);
        titleLabel.setFont(StyleUtil.getMetrolineFont(13));
        titleLabel.setForeground(StyleUtil.FOREGROUND_COLOR);

        closeButton = new JButton("×");
        closeButton.setFont(StyleUtil.getMetrolineFont(16));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setForeground(new Color(180, 180, 180));
        closeButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(new Color(180, 180, 180));
            }
        });
        closeButton.addActionListener(e -> hidePanel());

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(closeButton, BorderLayout.EAST);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        // Обработчики для перетаскивания за заголовок
        headerPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getLocationOnScreen();
                panelStartPoint = getLocation();
            }
        });

        headerPanel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null && panelStartPoint != null) {
                    Point currentPoint = e.getLocationOnScreen();
                    int deltaX = currentPoint.x - dragStartPoint.x;
                    int deltaY = currentPoint.y - dragStartPoint.y;
                    setLocation(panelStartPoint.x + deltaX, panelStartPoint.y + deltaY);
                }
            }
        });

        // Основная информация
        infoLabel = new JLabel();
        infoLabel.setFont(StyleUtil.getMetrolineFont(11));
        infoLabel.setForeground(new Color(200, 200, 200));

        // Прогресс-бар
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(120, 14));
        progressBar.setForeground(new Color(79, 155, 155));
        progressBar.setBackground(new Color(60, 60, 60));
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(infoLabel, BorderLayout.CENTER);
        contentPanel.add(progressBar, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);

        // Таймер для обновления информации
        updateTimer = new Timer(200, e -> updateInfo());
        updateTimer.start();

        // Фиксированный размер с учетом возможного прогресс-бара
        setPreferredSize(new Dimension(200, 130));
        setMinimumSize(new Dimension(200, 130));
    }

    public void displayStationInfo(Station station) {
        currentObject = station;
        updateInfo();
        setVisible(true);
    }

    public void displayTunnelInfo(Tunnel tunnel) {
        currentObject = tunnel;
        updateInfo();
        setVisible(true);
    }

    private void updateInfo() {
        if (currentObject instanceof Station) {
            Station station = (Station) currentObject;
            titleLabel.setText(station.getName());

            StringBuilder info = new StringBuilder("<html>");
            info.append("<b>Position:</b> ").append(station.getX()).append(", ").append(station.getY()).append("<br>");
            info.append("<b>Type:</b> ").append(formatStationType(station.getType())).append("<br>");
            info.append("<b>Color:</b> #").append(String.format("%06X", 0xFFFFFF & station.getColor().getRGB()));

            if (station.getType() == StationType.BUILDING) {
                info.append("<br><b>Cost:</b> ").append(NumberFormat.getIntegerInstance().format(50000)).append("₽");
            }

            if (!station.getConnections().isEmpty()) {
                info.append("<br><b>Connections:</b> ").append(station.getConnections().size());
            }

            infoLabel.setText(info.toString());
            updateProgress();
        } else if (currentObject instanceof Tunnel) {
            Tunnel tunnel = (Tunnel) currentObject;
            titleLabel.setText("Tunnel");

            StringBuilder info = new StringBuilder("<html>");
            info.append("<b>From:</b> ").append(tunnel.getStart().getName()).append("<br>");
            info.append("<b>To:</b> ").append(tunnel.getEnd().getName()).append("<br>");
            info.append("<b>Length:</b> ").append(tunnel.getLength()).append(" segments<br>");
            info.append("<b>Type:</b> ").append(formatTunnelType(tunnel.getType()));

            if (tunnel.getType() == TunnelType.BUILDING) {
                int cost = tunnel.getLength() * 10000;
                info.append("<br><b>Cost:</b> ").append(NumberFormat.getIntegerInstance().format(cost)).append("₽");
            }

            infoLabel.setText(info.toString());
            updateProgress();
        }
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

    private String formatStationType(StationType type) {
        switch (type) {
            case REGULAR: return "Regular";
            case TRANSFER: return "Transfer";
            case TERMINAL: return "Terminal";
            case BUILDING: return "Building";
            case DESTROYED: return "Destroying";
            default: return type.toString();
        }
    }

    private String formatTunnelType(TunnelType type) {
        switch (type) {
            case ACTIVE: return "Active";
            case PLANNED: return "Planned";
            case BUILDING: return "Building";
            case DESTROYED: return "Destroying";
            default: return type.toString();
        }
    }

    public void hidePanel() {
        currentObject = null;
        setVisible(false);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            updateInfo();
        }
    }
}