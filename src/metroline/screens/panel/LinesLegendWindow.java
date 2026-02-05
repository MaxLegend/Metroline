package metroline.screens.panel;

import metroline.core.world.World;
import metroline.objects.enums.StationColors;
import metroline.util.MetroLogger;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.StyleUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class LinesLegendWindow extends JWindow {


    private JPanel contentPanel;
    private JPanel headerPanel;
    private JButton closeButton;
    private JPanel linesPanel;
    private Map<StationColors, JPanel> colorEntries = new HashMap<>();

    public LinesLegendWindow(Window owner) {
        super(owner);
        setBackground(new Color(0, 0, 0, 0));

        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 30, 240));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.setColor(new Color(80, 80, 80, 150));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2d.dispose();
            }
        };

        contentPanel.setLayout(new BorderLayout(5, 5));
        contentPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        contentPanel.setOpaque(false);

        // Header panel with close button
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(LngUtil.translatable("legend.title"), SwingConstants.LEFT);
        titleLabel.setFont(StyleUtil.getMetrolineFont(13));
        titleLabel.setForeground(StyleUtil.FOREGROUND_COLOR);

        closeButton = new JButton("×");
        closeButton.setFont(StyleUtil.getMetrolineFont(14));
        closeButton.setForeground(StyleUtil.FOREGROUND_COLOR);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.addActionListener(e -> hideWindow());

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(closeButton, BorderLayout.EAST);

        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        // Lines panel
        linesPanel = new JPanel();
        linesPanel.setLayout(new BoxLayout(linesPanel, BoxLayout.Y_AXIS));
        linesPanel.setOpaque(false);
        linesPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(linesPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(300, 160));

        StyleUtil.MetroScrollBarUI.styleScrollPane(scrollPane);


        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(contentPanel);
        pack();

        // Add drag functionality
        MouseAdapter dragAdapter = new MouseAdapter() {
            private Point dragStartPoint;

            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getLocationOnScreen();
            }

            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    Point currentPoint = e.getLocationOnScreen();
                    int deltaX = currentPoint.x - dragStartPoint.x;
                    int deltaY = currentPoint.y - dragStartPoint.y;
                    int newX = getLocation().x + deltaX;
                    int newY = getLocation().y + deltaY;
                    Rectangle bounds = getAdjustedBounds(newX, newY);
                    setLocation(bounds.x, bounds.y);
                    dragStartPoint = currentPoint;
                }
            }
        };

        headerPanel.addMouseListener(dragAdapter);
        headerPanel.addMouseMotionListener(dragAdapter);
        titleLabel.addMouseListener(dragAdapter);
        titleLabel.addMouseMotionListener(dragAdapter);
    }


    public void updateLegend(World world) {
        if (SwingUtilities.isEventDispatchThread()) {
            updateLines(world);
        } else {
            SwingUtilities.invokeLater(() -> updateLines(world));
        }
    }
    public void updateLines(World world) {
        linesPanel.removeAll();
        colorEntries.clear();

        for (StationColors color : StationColors.values()) {
            boolean hasStations = world.getStations().stream()
                                       .anyMatch(s -> s.getStationColor() == color);

            if (hasStations) {
                addColorEntry(color, world); // Pass world here
            }
        }

        linesPanel.revalidate();
        linesPanel.repaint();
        pack();
    }

    private void addColorEntry(StationColors color, World world) {
        JPanel entryPanel = new JPanel(new BorderLayout(10, 0));
        entryPanel.setOpaque(false);
        entryPanel.setBorder(BorderFactory.createEmptyBorder());

        JPanel colorIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color.getColor());
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(0, getHeight()/2, getWidth()-10, getHeight()/2);
                g2d.fillOval(getWidth()-15, getHeight()/2-5, 10, 10);
            }
        };
        colorIndicator.setPreferredSize(new Dimension(50, 20));

        // Get custom name directly from world parameter
        String displayName = getLineDisplayName(color, world);
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(StyleUtil.getMetrolineFont(12));
        nameLabel.setForeground(StyleUtil.FOREGROUND_COLOR);

        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editLineName(color, nameLabel, world); // Pass world here
                }
            }
        });

        entryPanel.add(colorIndicator, BorderLayout.WEST);
        entryPanel.add(nameLabel, BorderLayout.CENTER);
        linesPanel.add(entryPanel);

        colorEntries.put(color, entryPanel);
    }
    private String getLineDisplayName(StationColors color, World world) {
        if (world != null) {
            String customName = world.getCustomLineName(color);
            if (customName != null && !customName.isEmpty()) {
                return customName;
            }
        }
        return LngUtil.translatable("line." + color.name().toLowerCase());
    }



    private void editLineName(StationColors color, JLabel nameLabel, World world) {
        JTextField editField = new JTextField(nameLabel.getText());
        editField.setFont(StyleUtil.getMetrolineFont(12));
        editField.setForeground(StyleUtil.FOREGROUND_COLOR);
        editField.setBackground(new Color(30, 30, 30, 240));
        editField.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));

        JPanel entryPanel = colorEntries.get(color);
        entryPanel.removeAll();

        editField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelEditing(color, nameLabel, entryPanel);
                }
            }
        });

        editField.addActionListener(e -> {
            String newName = editField.getText().trim();

            if (world != null) {
                world.setCustomLineName(color, newName);
                metroline.util.MetroLogger.logInfo("[LinesLegendWindow::editLineName] Saved line name for " +
                        color.name() + ": " + newName);
            }

            nameLabel.setText(newName.isEmpty() ?
                    LngUtil.translatable("line." + color.name().toLowerCase()) : newName);
            entryPanel.removeAll();
            entryPanel.add(createColorIndicator(color), BorderLayout.WEST);
            entryPanel.add(nameLabel, BorderLayout.CENTER);
            entryPanel.revalidate();
            entryPanel.repaint();
        });

        entryPanel.add(createColorIndicator(color), BorderLayout.WEST);
        entryPanel.add(editField, BorderLayout.CENTER);
        entryPanel.revalidate();
        entryPanel.repaint();

        editField.requestFocusInWindow();
        editField.selectAll();
    }
    private void cancelEditing(StationColors color, JLabel nameLabel, JPanel entryPanel) {
        entryPanel.removeAll();

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.add(nameLabel, BorderLayout.CENTER);

        // Add empty panel to maintain layout (confirm button will be hidden)
        rightPanel.add(new JPanel(), BorderLayout.EAST);

        entryPanel.add(createColorIndicator(color), BorderLayout.WEST);
        entryPanel.add(rightPanel, BorderLayout.CENTER);
        entryPanel.revalidate();
        entryPanel.repaint();
    }
    private JPanel createColorIndicator(StationColors color) {
        JPanel indicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
          //      super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color.getColor());
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(0, getHeight()/2, getWidth()-10, getHeight()/2);
                g2d.fillOval(getWidth()-15, getHeight()/2-5, 10, 10);
            }
        };
        indicator.setPreferredSize(new Dimension(50, 20));
        return indicator;
    }

    public void hideWindow() {
        setVisible(false);
    }

    public void showWindow() {
        setVisible(true);
        toFront();
    }
    public void disposeWindow() {
        hideWindow();
        dispose();
    }
    private Rectangle getAdjustedBounds(int x, int y) {
        Window owner = getOwner();
        if (owner == null) {
            return new Rectangle(x, y, getWidth(), getHeight());
        }

        Rectangle ownerBounds = owner.getBounds();
        int maxX = ownerBounds.x + ownerBounds.width - getWidth();
        int maxY = ownerBounds.y + ownerBounds.height - getHeight();

        int adjustedX = Math.max(ownerBounds.x, Math.min(x, maxX));
        int adjustedY = Math.max(ownerBounds.y, Math.min(y, maxY));

        return new Rectangle(adjustedX, adjustedY, getWidth(), getHeight());
    }

}
