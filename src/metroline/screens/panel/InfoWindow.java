package metroline.screens.panel;

import metroline.MainFrame;

import metroline.core.time.ConstructionTimeProcessor;
import metroline.core.time.GameTime;
import metroline.core.world.World;
import metroline.core.world.economic.EconomyManager;
import metroline.objects.enums.TrainType;
import metroline.objects.gameobjects.*;
import metroline.objects.gameobjects.StationLabel;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.util.MetroLogger;
import metroline.util.localizate.LngUtil;
import metroline.util.MathUtil;
import metroline.util.ui.UserInterfaceUtil;
import metroline.util.ui.StyleUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;


import metroline.core.world.GameWorld;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO При определенных, пока неустановленных условиях активированное окно для ввода может повесить игру
 * TODO Добавить редактирование названия и через метку тоже
 * TODO Ремонт станций доделать
 */
public class InfoWindow extends JWindow {
    private JLabel titleLabel;
    private JLabel infoLabel;
    private JProgressBar progressBar;
    private Timer updateTimer;
    public Object currentObject;
    private JPanel headerPanel;
    private JPanel contentPanel;
    private JButton closeButton;
    private JPanel editNamePanel;
    private JTextField nameEditField;
    private JButton saveNameButton;
    private JButton repairButton;
    private JLabel wearInfoLabel;

    private JButton toggleLabelButton;
    private boolean isUpdating = false;

    private static final Map<Object, InfoWindow> openWindows = new HashMap<>();
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

        // Кнопка ремонта
        repairButton = new JButton("\uD83D\uDD27");
        repairButton.setFont(StyleUtil.getMetrolineFont(14));
        repairButton.setForeground(StyleUtil.FOREGROUND_COLOR);
        repairButton.setContentAreaFilled(false);
        repairButton.setBorderPainted(false);
        repairButton.setFocusPainted(false);
        repairButton.setMargin(new Insets(0, 0, 0, 0));
        repairButton.addActionListener(e -> repairStation());

        toggleLabelButton = new JButton("👁");
        toggleLabelButton.setFont(StyleUtil.getMetrolineFont(14));
        toggleLabelButton.setForeground(StyleUtil.FOREGROUND_COLOR);
        toggleLabelButton.setContentAreaFilled(false);
        toggleLabelButton.setBorderPainted(false);
        toggleLabelButton.setFocusPainted(false);
        toggleLabelButton.setMargin(new Insets(0, 0, 0, 0));
        toggleLabelButton.addActionListener(e -> toggleLabelVisibility());


        // НОВЫЙ КОД: Создаем правую панель для дополнительных элементов
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(toggleLabelButton);
        rightPanel.add(repairButton);
        rightPanel.add(closeButton);

        toggleLabelButton.setVisible(false);
        // Собираем заголовок с новой структурой
        JPanel titlePanel = new JPanel(new BorderLayout(10, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.CENTER); // Текст слева
        titlePanel.add(rightPanel, BorderLayout.EAST);   // Кнопки справа

        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

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

        // Основная компоновка контента
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(infoLabel, BorderLayout.CENTER);
        contentPanel.add(progressBar, BorderLayout.SOUTH);

        setContentPane(contentPanel);

        // Таймер для обновления информации
        updateTimer = new Timer(200, e -> updateInfo());
        updateTimer.start();
     //   initLabelToggleButton();
        initNameEditComponents();
        //     initWearComponents();
        // Установка начального размера
        pack();
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .addPropertyChangeListener("activeWindow", evt -> {
                                Window activeWindow = (Window)evt.getNewValue();
                                if (activeWindow == getOwner()) {
                                    // Главное окно получило фокус - показываем поверх
                                    bringToFrontProperly();
                                } else if (isVisible()) {
                                    // Другое окно получило фокус - убираем alwaysOnTop
                                    setAlwaysOnTop(false);
                                }
                            });
    }
    private void repairStation() {
        if (currentObject instanceof Station) {
            Station station = (Station) currentObject;

            // Проверяем, можно ли ремонтировать станцию
            if (!station.canRepair()) {
                showRepairMessage("infoWnd.cannot_repair");
                return;
            }

            // Проверяем достаточно ли денег
            if (getOwner() instanceof MainFrame) {
                MainFrame frame = (MainFrame) getOwner();
                if (frame.getCurrentScreen() instanceof GameWorldScreen) {
                    GameWorldScreen screen = (GameWorldScreen) frame.getCurrentScreen();
                    GameWorld world = (GameWorld) screen.getWorld();

                    float repairCost = station.getRepairCost();
                    if (world.canAfford(repairCost)) {
                        // Производим ремонт
                        world.deductMoney(repairCost);
                        station.repair();

                        // Обновляем информацию
                        updateInfo();

                        // Показываем сообщение об успешном ремонте
                        showRepairMessage("infoWnd.repair_success", repairCost);
                    } else {
                        showRepairMessage("infoWnd.not_enough_money", repairCost);
                    }
                }
            }
        }
    }

    private void showRepairMessage(String messageKey, Object... args) {
        String message = LngUtil.translatable(messageKey, args);

        // Можно использовать всплывающее сообщение или изменить текст в infoLabel
        infoLabel.setText("<html>" + message + "<br>" + infoLabel.getText().replace("<html>", "") + "</html>");

        // Или показать временное сообщение
        JOptionPane.showMessageDialog(this, message,
                LngUtil.translatable("infoWnd.repair_title"),
                JOptionPane.INFORMATION_MESSAGE);
    }
    private void toggleLabelVisibility() {
        if (currentObject instanceof Station) {
            Station station = (Station) currentObject;
            World world = station.getWorld();
            StationLabel label = station.getLabel();

            if (label != null) {
                label.setVisible(!label.isVisible());

                // Обновляем иконку кнопки
                updateLabelToggleButtonIcon();

                // Перерисовываем экран
                if (getOwner() instanceof MainFrame) {
                    ((MainFrame)getOwner()).getCurrentScreen().repaint();
                }
            }
        }
    }

    private void updateLabelToggleButtonIcon() {
        if (currentObject instanceof Station) {
            Station station = (Station) currentObject;
            StationLabel label = station.getLabel();

            if (label != null) {
                if (label.isVisible()) {
                    toggleLabelButton.setText("◈");
                    toggleLabelButton.setToolTipText(LngUtil.translatable("infoWnd.hide_label"));
                } else {
                    toggleLabelButton.setText("◇");
                    toggleLabelButton.setToolTipText(LngUtil.translatable("infoWnd.show_label"));
                }
            }
        }
    }

    private void bringToFrontProperly() {
        if (getOwner() != null) {
            // 1. Сбрасываем alwaysOnTop
            setAlwaysOnTop(false);

            // 2. Устанавливаем правильный порядок
            toFront();

            // 3. Если владелец в фокусе - делаем поверх других окон приложения
            if (getOwner().isFocused()) {
                setAlwaysOnTop(true);
            }
        }
    }
    private void initNameEditComponents() {

        // Панель для редактирования имени
        editNamePanel = new JPanel(new BorderLayout(5, 0));
        editNamePanel.setOpaque(false);
        editNamePanel.setVisible(false);


        nameEditField = new JTextField();
        nameEditField.setFont(StyleUtil.getMetrolineFont(13));
        nameEditField.setForeground(StyleUtil.FOREGROUND_COLOR);
        nameEditField.setBackground(new Color(60, 60, 60));
        nameEditField.setBorder(BorderFactory.createCompoundBorder());

        saveNameButton = new JButton("\uD83D\uDCBE");
        saveNameButton.setFont(StyleUtil.getMetrolineFont(14));
        saveNameButton.setForeground(Color.WHITE);
        saveNameButton.setContentAreaFilled(false);
        saveNameButton.setBorderPainted(false);
        saveNameButton.setFocusPainted(false);
        saveNameButton.setMargin(new Insets(0, 5, 0, 0));
        saveNameButton.addActionListener(e -> saveStationName());
        nameEditField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelNameEditing();
                }
            }
        });
        nameEditField.addActionListener(e -> saveStationName());

        editNamePanel.add(nameEditField, BorderLayout.CENTER);
        editNamePanel.add(saveNameButton, BorderLayout.EAST);

        // Добавляем обработчик двойного клика на название станции
        MouseAdapter dragAdapter = new MouseAdapter() {
            private Point dragStartPoint;
            private boolean isDragging = false;

            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getLocationOnScreen();
                isDragging = false;
            }

            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {

                    isDragging = true;
                    Point currentPoint = e.getLocationOnScreen();
                    int deltaX = currentPoint.x - dragStartPoint.x;
                    int deltaY = currentPoint.y - dragStartPoint.y;
                    int newX = getLocation().x + deltaX;
                    int newY = getLocation().y + deltaY;
                    Rectangle bounds = getAdjustedBounds(newX, newY);

                    // Устанавливаем новое положение
                    setLocation(bounds.x, bounds.y);
                    dragStartPoint = currentPoint;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    e.consume(); // Отменяем другие события если было перетаскивание
                }
            }
        };

        // Особый обработчик для текстового поля
        nameEditField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Разрешаем перетаскивание только если текст не выделен
                if (nameEditField.getSelectedText() == null || nameEditField.getSelectedText().isEmpty()) {
                    dragAdapter.mousePressed(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragAdapter.mouseReleased(e);
            }
        });

        nameEditField.addMouseMotionListener(dragAdapter);
        saveNameButton.addMouseListener(dragAdapter);
        saveNameButton.addMouseMotionListener(dragAdapter);

        // Обработчик для двойного клика на заголовке
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentObject instanceof Station) {
                    startNameEditing((Station) currentObject);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                dragAdapter.mousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragAdapter.mouseReleased(e);
            }
        });

        titleLabel.addMouseMotionListener(dragAdapter);
        contentPanel.addMouseListener(dragAdapter);
        contentPanel.addMouseMotionListener(dragAdapter);
        headerPanel.addMouseListener(dragAdapter);
        headerPanel.addMouseMotionListener(dragAdapter);

        editNamePanel.add(nameEditField, BorderLayout.CENTER);
        editNamePanel.add(saveNameButton, BorderLayout.EAST);

    }

    private Rectangle getAdjustedBounds(int x, int y) {
        Window owner = getOwner();
        if (owner == null) {
            return new Rectangle(x, y, getWidth(), getHeight());
        }

        // Получаем границы MainFrame
        Rectangle ownerBounds = owner.getBounds();
        int maxX = ownerBounds.x + ownerBounds.width - getWidth();
        int maxY = ownerBounds.y + ownerBounds.height - getHeight();

        // Корректируем координаты
        int adjustedX = Math.max(ownerBounds.x, Math.min(x, maxX));
        int adjustedY = Math.max(ownerBounds.y, Math.min(y, maxY));

        return new Rectangle(adjustedX, adjustedY, getWidth(), getHeight());
    }
    private void cancelNameEditing() {
        JPanel titlePanel = (JPanel) editNamePanel.getParent();
        titlePanel.remove(editNamePanel);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        editNamePanel.setVisible(false);
        titlePanel.revalidate();
        titlePanel.repaint();
    }
    private void startNameEditing(Station station) {

        JPanel titlePanel = (JPanel) titleLabel.getParent();
        titlePanel.remove(titleLabel);
        titlePanel.add(editNamePanel, BorderLayout.CENTER);

        nameEditField.setText(station.getName());
        editNamePanel.setVisible(true);
        editNamePanel.setSize(0, titleLabel.getHeight());
        editNamePanel.setVisible(true);

        Timer animTimer = new Timer(10, new ActionListener() {
            int width = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (width < titlePanel.getWidth()) {
                    width += 20;
                    editNamePanel.setSize(width, titleLabel.getHeight());
                    titlePanel.revalidate();
                } else {
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        animTimer.start();
        titlePanel.revalidate();
        titlePanel.repaint();

        nameEditField.requestFocusInWindow();
        nameEditField.selectAll();


    }
    private boolean isEditingName = false;
    private void saveStationName() {
        if (isEditingName || !(currentObject instanceof Station)) return;
        MetroLogger.logInfo("START saveStationName");
        isEditingName = true;
        try {
            Station station = (Station) currentObject;
            String newName = nameEditField.getText().trim();

            if (!newName.isEmpty() && !newName.equals(station.getName())) {
                station.setName(newName);
                titleLabel.setText(newName);

                // Убираем вызов updateInfo() - он вызывает рекурсию!
                // updateInfo(); // ← УБРАТЬ эту строку!

                // Просто обновляем заголовок
                Color stationColor = station.getStationColor().getColor();
                titleLabel.setText("<html><font color='" +
                        String.format("#%06X", stationColor.getRGB() & 0xFFFFFF) + "'>" +
                        newName + "</font></html>");
                MetroLogger.logInfo("saveStationName " + newName);
            }

            cancelNameEditing();

            // Только перерисовка, без обновления информации
            if (getOwner() instanceof MainFrame) {
                ((MainFrame)getOwner()).getCurrentScreen().repaint();
                MetroLogger.logInfo("repaint ");
            }
        } finally {
            isEditingName = false;
        }
    }
    public static void updateWindowsVisibility(MainFrame frame) {
        boolean shouldShow = frame.getCurrentScreen() instanceof WorldScreen ;
        for (Window window : frame.getOwnedWindows()) {
            if (window instanceof InfoWindow) {
                window.setVisible(shouldShow);
            }
        }
    }
    public void displayTrainInfo(Train train, Point location) {
        toggleLabelButton.setVisible(false);
        this.currentObject = train;
        updateInfo();
        setLocation(location);
        setVisible(true);
        pack();
    }

    public void displayStationInfo(Station station, Point location) {
        if (openWindows.containsKey(station)) {
            InfoWindow existingWindow = openWindows.get(station);
            // Обновляем позицию и показываем существующее окно
            existingWindow.setLocation(location);
            existingWindow.updateInfo();
            existingWindow.setVisible(true);
            existingWindow.toFront();
            return;
        }

    this.currentObject = station;
    if (editNamePanel.isVisible()) {
        saveStationName();
    }
        // Добавляем в карту открытых окон
        openWindows.put(station, this);


    Color stationColor = station.getStationColor().getColor();
    titleLabel.setText("<html><font color='" +
            String.format("#%06X", stationColor.getRGB() & 0xFFFFFF) + "'>" +
            station.getName() + "</font></html>");

    // Показываем кнопку переключения видимости метки
    toggleLabelButton.setVisible(true);
    updateLabelToggleButtonIcon();

    // Добавляем кнопку в заголовок
    JPanel titlePanel = (JPanel) titleLabel.getParent();
    if (titlePanel != null && !Arrays.asList(titlePanel.getComponents()).contains(toggleLabelButton)) {
        titlePanel.add(toggleLabelButton, BorderLayout.WEST);
    }

    updateInfo();
    setLocation(location);
    setVisible(true);
    pack();
}
    public void displayTunnelInfo(Tunnel tunnel, Point location) {
        if (openWindows.containsKey(tunnel)) {
            InfoWindow existingWindow = openWindows.get(tunnel);
            // Обновляем позицию и показываем существующее окно
            existingWindow.setLocation(location);
            existingWindow.updateInfo();
            existingWindow.setVisible(true);
            existingWindow.toFront();
            return;
        }
        openWindows.put(tunnel, this);

        toggleLabelButton.setVisible(false);
        repairButton.setVisible(false);
        this.currentObject = tunnel;
        updateInfo();
        setLocation(location);
        setVisible(true);
        pack(); // Обновляем размер окна под содержимое
    }
    public void displayGameplayUnitsInfo(GameplayUnits gUnits, Point location) {
        if (openWindows.containsKey(gUnits)) {
            InfoWindow existingWindow = openWindows.get(gUnits);
            // Обновляем позицию и показываем существующее окно
            existingWindow.setLocation(location);
            existingWindow.updateInfo();
            existingWindow.setVisible(true);
            existingWindow.toFront();
            return;
        }
        openWindows.put(gUnits, this);
        toggleLabelButton.setVisible(false);
        repairButton.setVisible(false);
        this.currentObject = gUnits;
        updateInfo();
        setLocation(location);
        setVisible(true);
        pack(); // Обновляем размер окна под содержимое
    }
    public void displayLabelInfo(StationLabel stationLabel, Point location) {
        if (openWindows.containsKey(stationLabel)) {
            InfoWindow existingWindow = openWindows.get(stationLabel);
            // Обновляем позицию и показываем существующее окно
            existingWindow.setLocation(location);
            existingWindow.updateInfo();
            existingWindow.setVisible(true);
            existingWindow.toFront();
            return;
        }
        openWindows.put(stationLabel, this);
        repairButton.setVisible(false);
        toggleLabelButton.setVisible(false);
        this.currentObject = stationLabel;
        updateInfo();
        setLocation(location);
        setVisible(true);
        pack(); // Обновляем размер окна под содержимое
    }
    float revenue, lastRevenue;
    public void updateInfo() {
        if (isUpdating) return;
        isUpdating = true;
        // Получаем экран мира (может быть GameWorldScreen или SandboxWorldScreen)
        try {
            WorldScreen worldScreen = null;
            if (getOwner() instanceof MainFrame) {
                MainFrame frame = (MainFrame) getOwner();
                if (frame.getCurrentScreen() instanceof WorldScreen) {
                    worldScreen = (WorldScreen) frame.getCurrentScreen();
                }
            }

            if (worldScreen == null || !(worldScreen.getWorld() instanceof GameWorld)) {
                return;
            }

            GameWorld world = (GameWorld) worldScreen.getWorld();
            EconomyManager economyManager = world.getEconomyManager();

            if (currentObject instanceof Station) {
                Station station = (Station) currentObject;
                GameTime gameTime = world.getGameTime();
                long constructionDate = station.getConstructionDate();
                long currentTime = gameTime.getCurrentTimeMillis();
                long age = currentTime - constructionDate;

                Color stationColor = station.getStationColor().getColor();
                titleLabel.setText("<html><font color='" +
                        String.format("#%06X", stationColor.getRGB() & 0xFFFFFF) + "'>" +
                        station.getName() + "</font></html>");

                StringBuilder info = new StringBuilder("<html>");
                info.append(LngUtil.translatable("infoWnd.position") + " ").append(station.getX()).append(", ").append(station.getY()).append("<br>");
                info.append(LngUtil.translatable("infoWnd.type") + " ").append(station.getType().getLocalizedName()).append("<br>");

               float currentRevenue = economyManager.preCalculateStationRevenue(station);

                info.append(LngUtil.translatable("infoWnd.revenue") + " ")
                    .append(MathUtil.round(currentRevenue, 2))
                    .append(" M (")
                    .append(MathUtil.round((1 - station.getWearLevel()) * 100, 0))
                    .append("%)<br>");

                // Используем EconomyManager для расчета стоимости строительства
                float constructionCost = economyManager.calculateStationConstructionCost(station.getX(), station.getY());
                info.append(LngUtil.translatable("infoWnd.cost") + " ").append(MathUtil.round(constructionCost, 2)).append(" M").append("<br>");

                // Используем EconomyManager для расчета содержания
                float upkeep = economyManager.calculateStationUpkeep(station);
                info.append(LngUtil.translatable("infoWnd.upkeep") + " ").append(MathUtil.round(upkeep, 4)).append(" M").append("<br>");

                info.append(String.format("<html>%s: %s<br>%s: %s</html>",
                        LngUtil.translatable("infoWnd.station_build_date"),
                        gameTime.formatDate(constructionDate),
                        LngUtil.translatable("infoWnd.station_wear_level"),
                        String.format("%.0f%%", station.getWearLevel() * 100)));

                infoLabel.setText(info.toString());
                updateProgress(world);

            } else if (currentObject instanceof Train) {
                Train train = (Train) currentObject;
                titleLabel.setText(LngUtil.translatable("infoWnd.train.cost." + train.getTrainType().name().toLowerCase()));

                StringBuilder info = new StringBuilder("<html>");
                info.append(LngUtil.translatable("infoWnd.position") + " ").append(MathUtil.round(train.getCurrentX(), 2)).append(", ").append(MathUtil.round(train.getCurrentY(), 2)).append("<br>");
                info.append(LngUtil.translatable("infoWnd.train_speed") + " ").append(train.getNormalizedSpeed()).append("<br>");
                if (!train.isMoving())
                    info.append(LngUtil.translatable("infoWnd.train_wait_time") + " ").append(train.getWaitTimer() + " s").append("<br>");
                infoLabel.setText(info.toString());

            } else if (currentObject instanceof Tunnel) {
                Tunnel tunnel = (Tunnel) currentObject;
                titleLabel.setText(LngUtil.translatable("infoWnd.tunnel_title"));

                StringBuilder info = new StringBuilder("<html>");
                info.append(LngUtil.translatable("infoWnd.tunnel_from") + " ").append(tunnel.getStart().getName()).append("<br>");
                info.append(LngUtil.translatable("infoWnd.tunnel_to") + " ").append(tunnel.getEnd().getName()).append("<br>");
                info.append(LngUtil.translatable("infoWnd.tunnel_length") + " ").append(tunnel.getLength()).append(" " + LngUtil.translatable("infoWnd.tunnel_segments") + " <br>");
                info.append(LngUtil.translatable("infoWnd.tunnel_type") + " ").append(tunnel.getType().getLocalizedName() + "<br>");

                // Используем EconomyManager для расчета стоимости строительства
                float cost = economyManager.calculateTunnelConstructionCost(tunnel);
                info.append(LngUtil.translatable("infoWnd.tunnel_cost") + " ").append(MathUtil.round(cost, 2)).append(" M" + " <br>");

                // Используем EconomyManager для расчета содержания
                float upkeep = economyManager.calculateTunnelUpkeep(tunnel);
                info.append(LngUtil.translatable("infoWnd.upkeep") + " ").append(MathUtil.round(upkeep, 4)).append(" M").append("<br>");

                infoLabel.setText(info.toString());
                updateProgress(world);

            } else if (currentObject instanceof GameplayUnits) {
                GameplayUnits gUnits = (GameplayUnits) currentObject;
                titleLabel.setText(LngUtil.translatable(gUnits.getType().getLocalizedName()));

                StringBuilder info = new StringBuilder("<html>");
                info.append(LngUtil.translatable("infoWnd.gUnits") + " ").append(gUnits.getType().getIncomeMultiplier() + " %").append("<br>");
                info.append(LngUtil.translatable("infoWnd.position") + " ").append(gUnits.getX()).append(", ").append(gUnits.getY()).append("<br>");
                info.append(LngUtil.translatable("infoWnd.condition") + " ").append(gUnits.getCondition()).append("<br>");
                if (gUnits.isAbandoned())
                    info.append(LngUtil.translatable("infoWnd.isAbandoned") + " ").append(gUnits.isAbandoned()).append("<br>");
                infoLabel.setText(info.toString());

            } else if (currentObject instanceof StationLabel) {
                StationLabel stationLabel = (StationLabel) currentObject;
                titleLabel.setText(LngUtil.translatable(stationLabel.getText()));

                StringBuilder info = new StringBuilder("<html>");
                info.append(LngUtil.translatable("infoWnd.stationLabel") + " ").append(stationLabel.getText()).append("<br>");
                infoLabel.setText(info.toString());
            }

            pack(); // Подгоняем размер окна под содержимое
        } finally {
            isUpdating = false;
        }
    }

    private void updateProgress(GameWorld world) {
        ConstructionTimeProcessor processor = world.getConstructionProcessor();

        if (currentObject instanceof Station) {
            Station station = (Station) currentObject;
            float progress = processor.getStationConstructionProgress(station);
            if (progress > 0 && progress < 1) {
                progressBar.setVisible(true);
                progressBar.setValue((int)(progress * 100));
                progressBar.setString((int)(progress * 100) + "%");
            } else {
                progressBar.setVisible(false);
            }
        } else if (currentObject instanceof Tunnel) {
            Tunnel tunnel = (Tunnel) currentObject;
            float progress = processor.getTunnelConstructionProgress(tunnel);
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
        toggleLabelButton.setVisible(false);
        if (editNamePanel.isVisible()) {
            saveStationName();
        }
        // Удаляем из хранилища открытых окон
        if (currentObject != null) {
            openWindows.remove(currentObject);
        }

        currentObject = null;
        setVisible(false);
        if (getOwner() instanceof MainFrame) {
            MainFrame frame = (MainFrame) getOwner();
            if (frame.getCurrentScreen() instanceof GameWorldScreen) {
                GameWorldScreen screen = (GameWorldScreen) frame.getCurrentScreen();
                screen.infoWindows.remove(this);
            }
        }
    }
    @Override
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (currentObject != null) {
            openWindows.remove(currentObject);
        }
        super.dispose();
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