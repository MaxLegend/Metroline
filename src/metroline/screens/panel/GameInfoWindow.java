package metroline.screens.panel;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.*;
import metroline.screens.worldscreens.normal.GameClickController;
import metroline.util.MetroLogger;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolineCheckbox;
import metroline.util.ui.MetrolineLabel;
import metroline.util.ui.StyleUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.im.InputContext;
import java.util.HashMap;
import java.util.Map;

public class GameInfoWindow extends JWindow {

    private final GameObject targetObject;
    private final GameClickController controller;
    private MetrolineLabel titleLabel;
    private JPanel contentPanel;
    private JPanel fieldsPanel;
    private JTextField nameEditField;
    private MetrolineButton closeButton;
    private MetrolineButton addFieldButton;
    private MetrolineCheckbox closeAfterNameCheckbox;
    private MetrolineCheckbox stationLabelCheckbox;
    private static boolean closeAfterNameInput = true; // Shared preference

    private static final Map<Object, GameInfoWindow> openWindows = new HashMap<>();

    public GameInfoWindow(GameObject targetObject, int x, int y, GameClickController controller) {
        super(SwingUtilities.getWindowAncestor(controller.screen));

        this.targetObject = targetObject;
        this.controller = controller;

        if (openWindows.containsKey(targetObject)) {
            openWindows.get(targetObject).dispose();
        }
        openWindows.put(targetObject, this);

        initUI();
        positionWindow(x, y);
        setVisible(true);
    }

    private void initUI() {
        setBackground(new Color(0, 0, 0, 0));

        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 30, 245));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2d.setColor(new Color(70, 70, 70, 200));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2d.dispose();
            }
        };
        contentPanel.setLayout(new BorderLayout(0, 10));
        contentPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        contentPanel.setOpaque(false);

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        titleLabel = new MetrolineLabel("station_info.desc");
        titleLabel.setFont(StyleUtil.getMetrolineFont(14));
        titleLabel.setForeground(Color.WHITE);

        closeButton = new MetrolineButton("❌");

        closeButton.setForeground(Color.LIGHT_GRAY);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(null);
        closeButton.addActionListener(e -> dispose());
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(closeButton, BorderLayout.EAST);

        // --- DYNAMIC FIELDS CONTAINER ---
        fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setOpaque(false);
        fieldsPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Main Name Field
        addNameRow();

        if (targetObject instanceof Station station) {
            Map<String, String> existingProps = station.getCustomProperties();
            if (existingProps.isEmpty()) {
                addPropertyRow("Year", "1935");
                addPropertyRow("Creator", "Tesmio");
            } else {

                existingProps.forEach(this::addPropertyRow);
            }
        }

        // --- FOOTER (Add Button) ---
        addFieldButton = new MetrolineButton("+");
        addFieldButton.setFont(StyleUtil.getMetrolineFont(11));
        addFieldButton.setForeground(new Color(150, 200, 150));
        addFieldButton.setContentAreaFilled(false);
        addFieldButton.setFocusPainted(false);
        addFieldButton.setBorder(BorderFactory.createDashedBorder(new Color(150, 200, 150), 1, 1));
        addFieldButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addFieldButton.addActionListener(e -> {
            MetroLogger.logInfo("[GameInfoWindow::addFieldButton] Adding new property row");
            addPropertyRow("Key", "Value");
            refreshLayout();
        });

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(fieldsPanel, BorderLayout.CENTER);
        contentPanel.add(addFieldButton, BorderLayout.SOUTH);

        setContentPane(contentPanel);
        pack();

        MouseAdapter drag = createDragAdapter();
        contentPanel.addMouseListener(drag);
        contentPanel.addMouseMotionListener(drag);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                // Ð¡Ð¾Ñ…Ñ€Ð°Ð½ÑÐµÐ¼ Ð¿Ñ€Ð¸ Ð¿Ð¾Ñ‚ÐµÑ€Ðµ Ñ„Ð¾ÐºÑƒÑÐ° (Ð½Ð¾ Ð½Ðµ Ð¿Ñ€Ð¸ Ð·Ð°ÐºÑ€Ñ‹Ñ‚Ð¸Ð¸)
                if (isDisplayable()) {
                    saveData();
                }
            }
        });

        MainFrame.getInstance().translatables.add(titleLabel);
        titleLabel.updateTranslation();
    }
    private void addNameRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(400, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Column 0: Label "Name"
        JLabel label = new JLabel("Name:");
        label.setForeground(new Color(200, 200, 200));
        label.setPreferredSize(new Dimension(100, 28));
        gbc.weightx = 0.0;
        gbc.gridx = 0;
        row.add(label, gbc);

        // Column 1: Colon
        gbc.weightx = 0.0;
        gbc.gridx = 1;
        JLabel colon = new JLabel(":");
        colon.setForeground(Color.WHITE);
        row.add(colon, gbc);

        // Column 2: Name input field
        nameEditField = createStyledTextField(targetObject instanceof Station ?
                ((Station) targetObject).getName() : "Object");
        nameEditField.setPreferredSize(new Dimension(200, 28));

        // Use AbstractAction like in property fields
        AbstractAction saveNameAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveName();
            }
        };
        nameEditField.getInputMap(JComponent.WHEN_FOCUSED)
                     .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "saveNameAction");
        nameEditField.getActionMap().put("saveNameAction", saveNameAction);

        gbc.weightx = 1.0;
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(nameEditField, gbc);

        // Column 3: Checkbox "close after input"
        closeAfterNameCheckbox = StyleUtil.createMetrolineCheckBox("", "world.closeAfterNameCheckbox_desc");
        closeAfterNameCheckbox.setSelected(closeAfterNameInput);
        closeAfterNameCheckbox.setOpaque(false);

        closeAfterNameCheckbox.addActionListener(e -> closeAfterNameInput = closeAfterNameCheckbox.isSelected());
        gbc.weightx = 0.0;
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.NONE;
        MainFrame.getInstance().translatables.add(closeAfterNameCheckbox);
        closeAfterNameCheckbox.updateTranslation();
        row.add(closeAfterNameCheckbox, gbc);

        fieldsPanel.add(row);
        fieldsPanel.add(Box.createVerticalStrut(8));

        // NEW: Station Label row (only for stations)
        if (targetObject instanceof Station station) {
            addStationLabelRow(station);
        }
    }
//    private void addNameRow() {
//        JPanel row = new JPanel(new GridBagLayout());
//        row.setOpaque(false);
//        row.setMaximumSize(new Dimension(400, 35));
//
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.insets = new Insets(2, 2, 2, 2);
//
//        // Column 0: Label "Name"
//        JLabel label = new JLabel("Name:");
//        label.setForeground(new Color(200, 200, 200));
//        label.setPreferredSize(new Dimension(100, 28));
//        gbc.weightx = 0.0;
//        gbc.gridx = 0;
//        row.add(label, gbc);
//
//        // Column 1: Colon
//        gbc.weightx = 0.0;
//        gbc.gridx = 1;
//        JLabel colon = new JLabel(":");
//        colon.setForeground(Color.WHITE);
//        row.add(colon, gbc);
//
//        // Column 2: Name input field
//        nameEditField = createStyledTextField(targetObject instanceof Station ?
//                ((Station) targetObject).getName() : "Object");
//        nameEditField.setPreferredSize(new Dimension(200, 28));
//        nameEditField.getInputMap(JComponent.WHEN_FOCUSED)
//                     .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "saveNameAction");
//        nameEditField.getActionMap().put("saveNameAction", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                saveName();
//            }
//        });
//
//        gbc.weightx = 1.0;
//        gbc.gridx = 2;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        row.add(nameEditField, gbc);
//
//        // Column 3: Checkbox "close after input"
//        closeAfterNameCheckbox = StyleUtil.createMetrolineCheckBox("", "world.closeAfterNameCheckbox_desc");
//        closeAfterNameCheckbox.setSelected(closeAfterNameInput);
//        closeAfterNameCheckbox.setOpaque(false);
//
//        closeAfterNameCheckbox.addActionListener(e -> closeAfterNameInput = closeAfterNameCheckbox.isSelected());
//        gbc.weightx = 0.0;
//        gbc.gridx = 3;
//        gbc.fill = GridBagConstraints.NONE;
//        MainFrame.getInstance().translatables.add(closeAfterNameCheckbox);
//        closeAfterNameCheckbox.updateTranslation();
//        row.add(closeAfterNameCheckbox, gbc);
//
//        fieldsPanel.add(row);
//        fieldsPanel.add(Box.createVerticalStrut(8));
//
//        // NEW: Station Label row (only for stations)
//        if (targetObject instanceof Station station) {
//            addStationLabelRow(station);
//        }
//    }

    // NEW method
    private void addStationLabelRow(Station station) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(400, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Label
        JLabel label = new JLabel(LngUtil.translatable("stationInfo.showLabel"));
        label.setForeground(new Color(200, 200, 200));
        label.setPreferredSize(new Dimension(100, 28));
        gbc.weightx = 0.0;
        gbc.gridx = 0;
        row.add(label, gbc);

        // Colon
        gbc.gridx = 1;
        JLabel colon = new JLabel(":");
        colon.setForeground(Color.WHITE);
        row.add(colon, gbc);

        // Checkbox
        stationLabelCheckbox = StyleUtil.createMetrolineCheckBox("", LngUtil.translatable("stationInfo.showLabel_desc"));
        stationLabelCheckbox.setSelected(station.getLabel() != null);
        stationLabelCheckbox.setOpaque(false);
        stationLabelCheckbox.addActionListener(e -> handleLabelCheckbox(station));
        gbc.weightx = 1.0;
        gbc.gridx = 2;
        row.add(stationLabelCheckbox, gbc);

        fieldsPanel.add(row);
        fieldsPanel.add(Box.createVerticalStrut(8));
    }

    // NEW method
    private void handleLabelCheckbox(Station station) {
        if (stationLabelCheckbox.isSelected()) {
            // Create label if it doesn't exist
            if (station.getLabel() == null) {
                GameWorld world = (GameWorld) station.getWorld();
                StationLabel newLabel = new StationLabel(
                        world,
                        station.getX(),
                        station.getY() - 1,
                        station.getName(),station
                );
                newLabel.setParentGameObject(station);
                world.addLabel(newLabel);
                station.setLabel(newLabel);
                controller.screen.repaint();
            }
        } else {
            // Remove label if it exists
            StationLabel label = station.getLabel();
            if (label != null) {
                GameWorld world = (GameWorld) station.getWorld();
                world.removeLabel(label);
                station.setLabel(null);
                controller.screen.repaint();
            }
        }
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
    private void addPropertyRow(String key, String value) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(400, 35)); // Ð•Ð´Ð¸Ð½Ð°Ñ Ð²Ñ‹ÑÐ¾Ñ‚Ð° Ð´Ð»Ñ Ð²ÑÐµÑ… ÑÑ‚Ñ€Ð¾Ðº

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        // ÐšÐ¾Ð»Ð¾Ð½ÐºÐ° 0: ÐŸÐ¾Ð»Ðµ ÐºÐ»ÑŽÑ‡Ð° (Ñ„Ð¸ÐºÑÐ¸Ñ€Ð¾Ð²Ð°Ð½Ð½Ð°Ñ ÑˆÐ¸Ñ€Ð¸Ð½Ð°)
        JTextField keyField = createStyledTextField(key);
        keyField.setPreferredSize(new Dimension(100, 28)); // Ð¨Ð¸Ñ€Ð¸Ð½Ð° = Ð·Ð°Ð³Ð¾Ð»Ð¾Ð²ÐºÑƒ "Name:"
        row.putClientProperty("keyField", keyField);
        gbc.weightx = 0.0;
        gbc.gridx = 0;
        row.add(keyField, gbc);

        // ÐšÐ¾Ð»Ð¾Ð½ÐºÐ° 1: Ð”Ð²Ð¾ÐµÑ‚Ð¾Ñ‡Ð¸Ðµ
        gbc.weightx = 0.0;
        gbc.gridx = 1;
        JLabel colon = new JLabel(":");
        colon.setForeground(Color.WHITE);
        row.add(colon, gbc);

        // ÐšÐ¾Ð»Ð¾Ð½ÐºÐ° 2: ÐŸÐ¾Ð»Ðµ Ð·Ð½Ð°Ñ‡ÐµÐ½Ð¸Ñ
        JTextField valField = createStyledTextField(value);
        valField.setPreferredSize(new Dimension(200, 28)); // Ð¨Ð¸Ñ€Ð¸Ð½Ð° = Ð¿Ð¾Ð»ÑŽ Ð¸Ð¼ÐµÐ½Ð¸
        row.putClientProperty("valueField", valField);
        gbc.weightx = 1.0;
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(valField, gbc);

        // ÐšÐ¾Ð»Ð¾Ð½ÐºÐ° 3: ÐšÐ½Ð¾Ð¿ÐºÐ° ÑƒÐ´Ð°Ð»ÐµÐ½Ð¸Ñ
        JButton delBtn = new JButton("❌");
        delBtn.setForeground(new Color(200, 100, 100));
        delBtn.setContentAreaFilled(false);
        delBtn.setBorder(null);
        delBtn.setMargin(new Insets(0, 5, 0, 5));
        delBtn.setPreferredSize(new Dimension(28, 28)); // Ð¤Ð¸ÐºÑÐ¸Ñ€Ð¾Ð²Ð°Ð½Ð½Ñ‹Ð¹ Ñ€Ð°Ð·Ð¼ÐµÑ€ Ð´Ð»Ñ Ð²Ñ‹Ñ€Ð°Ð²Ð½Ð¸Ð²Ð°Ð½Ð¸Ñ
        delBtn.addActionListener(e -> {
            fieldsPanel.remove(row);
            saveData();
            revalidate();
            repaint();
        });
        gbc.weightx = 0.0;
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.NONE;
        row.add(delBtn, gbc);
        AbstractAction saveDataAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
            }
        };
        keyField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "saveDataAction");
        keyField.getActionMap().put("saveDataAction", saveDataAction);

        valField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "saveDataAction");
        valField.getActionMap().put("saveDataAction", saveDataAction);

        // Save on focus lost
        keyField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { saveData(); }
        });
        valField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { saveData(); }
        });

        fieldsPanel.add(row);
        fieldsPanel.add(Box.createVerticalStrut(8));
        revalidate();
    }
    private JTextField createStyledTextField(String text) {
        JTextField f = new JTextField(text) {
            @Override
            public InputContext getInputContext() {
                // Return parent's input context to avoid IME issues on dispose
                Window owner = SwingUtilities.getWindowAncestor(this);
                if (owner != null && owner.getOwner() != null) {
                    return owner.getOwner().getInputContext();
                }
                return super.getInputContext();
            }
        };
        f.setFont(StyleUtil.getMetrolineFont(12));
        f.setForeground(Color.WHITE);
        f.setBackground(new Color(50, 50, 50));
        f.setCaretColor(Color.YELLOW);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        return f;
    }

    private void saveName() {
        saveData();
        if (closeAfterNameInput) {
            dispose();
        }
    }

    private void refreshLayout() {
        fieldsPanel.revalidate();
        fieldsPanel.repaint();
        pack();
    }

    private void positionWindow(int x, int y) {
        Point screenPoint = controller.screen.worldToScreen(x, y);
        Point windowPoint = SwingUtilities.convertPoint(controller.screen, screenPoint.x, screenPoint.y, null);
        setLocation(windowPoint.x + 15, windowPoint.y + 15);
    }

    private MouseAdapter createDragAdapter() {
        return new MouseAdapter() {
            private Point start;
            @Override public void mousePressed(MouseEvent e) { start = e.getLocationOnScreen(); }
            @Override      public void mouseDragged(MouseEvent e) {
                if (start != null) {
                    Point currentPoint = e.getLocationOnScreen();
                    int deltaX = currentPoint.x - start.x;
                    int deltaY = currentPoint.y - start.y;
                    int newX = getLocation().x + deltaX;
                    int newY = getLocation().y + deltaY;
                    Rectangle bounds = getAdjustedBounds(newX, newY);
                    setLocation(bounds.x, bounds.y);
                    start = currentPoint;
                }
            }
        };
    }

    @Override
    public void dispose() {
        saveData();
        MetroLogger.logInfo("[GameInfoWindow::dispose] Window closed");
        MainFrame.getInstance().translatables.remove(titleLabel);
        MainFrame.getInstance().translatables.remove(closeAfterNameCheckbox);
        openWindows.remove(targetObject);

        // Fix: Return focus to parent window BEFORE dispose to prevent IME reset
        Window owner = getOwner();
        if (owner != null) {
            owner.requestFocus();
        }

        // Disable IME on text fields before closing
        if (nameEditField != null) {
            nameEditField.enableInputMethods(false);
        }

        super.dispose();
    }
    private void saveData() {
        if (targetObject instanceof Station station) {
            // 1. Ð¡Ð¾Ñ…Ñ€Ð°Ð½ÑÐµÐ¼ Ð¸Ð¼Ñ
            if (nameEditField != null) {
                station.setName(nameEditField.getText().trim());
            }

            // 2. Ð¡Ð¾Ð±Ð¸Ñ€Ð°ÐµÐ¼ ÐºÐ°ÑÑ‚Ð¾Ð¼Ð½Ñ‹Ðµ ÑÐ²Ð¾Ð¹ÑÑ‚Ð²Ð°
            Map<String, String> properties = new HashMap<>();
            for (Component comp : fieldsPanel.getComponents()) {
                if (comp instanceof JPanel rowPanel) {
                    // Ð¢ÐµÐ¿ÐµÑ€ÑŒ getClientProperty Ð²ÐµÑ€Ð½ÐµÑ‚ Ð¾Ð±ÑŠÐµÐºÑ‚Ñ‹, Ð° Ð½Ðµ null
                    JTextField keyField = (JTextField) rowPanel.getClientProperty("keyField");
                    JTextField valueField = (JTextField) rowPanel.getClientProperty("valueField");

                    if (keyField != null && valueField != null) {
                        String key = keyField.getText().trim();
                        String value = valueField.getText().trim();

                        // ÐŸÑ€Ð¾Ð¿ÑƒÑÐºÐ°ÐµÐ¼ Ð¿ÑƒÑÑ‚Ñ‹Ðµ ÐºÐ»ÑŽÑ‡Ð¸ Ð¸ ÑÐ¸ÑÑ‚ÐµÐ¼Ð½Ñ‹Ðµ Ð¿Ð¾Ð»Ñ
                        if (!key.isEmpty() && !key.equals("ÐšÐ»ÑŽÑ‡")) {
                            properties.put(key, value);

                        }
                    }
                }
            }

            // 3. Ð¡Ð¾Ñ…Ñ€Ð°Ð½ÑÐµÐ¼ Ð² Ð¾Ð±ÑŠÐµÐºÑ‚ ÑÑ‚Ð°Ð½Ñ†Ð¸Ð¸
            station.setCustomProperties(properties);

            // ÐŸÐµÑ€ÐµÑ€Ð¸ÑÐ¾Ð²Ñ‹Ð²Ð°ÐµÐ¼ Ð¼Ð¸Ñ€, Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð¸Ð·Ð¼ÐµÐ½ÐµÐ½Ð¸Ñ (Ð½Ð°Ð¿Ñ€Ð¸Ð¼ÐµÑ€, Ð¸Ð¼Ñ) Ð¾Ñ‚Ð¾Ð±Ñ€Ð°Ð·Ð¸Ð»Ð¸ÑÑŒ
            controller.screen.repaint();
        }
    }

    public static void closeAll() {
        for (GameInfoWindow w : new java.util.ArrayList<>(openWindows.values())) w.dispose();
    }
}