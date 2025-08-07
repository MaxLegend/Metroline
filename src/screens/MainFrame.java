package screens;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import game.core.world.SandboxWorld;
import util.MetroLogger;
import util.StyleUtil;

/**
 * Main application frame that contains all game screens and toolbar
 * TODO: прекращает работать сохранение картинок или загрузка если что то уже было выполнено в мире
 * @author Tesmio
 */
public class MainFrame extends JFrame {

    public static final String SANDBOX_SCREEN_NAME = "sandbox_gamescreen";
    public static final String MAIN_MENU_SCREEN_NAME = "menu";
    public static final String GAME_SCREEN_NAME = "gamescreen";
    public static final String WORLD_SETTINGS_SCREEN_NAME = "world_settings";

    private GameScreen currentScreen;
    private JToolBar toolBar;
    private Map<String, GameScreen> screens = new HashMap<>();
    private boolean isFullscreen = false;
    private JLabel timeLabel = new JLabel("00:00 01.01.0000");
    private JPanel timePanel = new JPanel();

    private String currentTimeString = "00:00 01.01.0000";
    // Настройки стиля
    static {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            Color darkBg = new Color(45, 45, 45);
            UIManager.put("OptionPane.background", darkBg);
            UIManager.put("Panel.background", darkBg);
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * MainFrame constructor
     */
    public MainFrame() {
        super("Metroline");
        if (screens.isEmpty()) {
            screens.put(MAIN_MENU_SCREEN_NAME, new MenuScreen(this));
            screens.put(SANDBOX_SCREEN_NAME, new WorldSandboxScreen(this));
            screens.put(GAME_SCREEN_NAME, new WorldGameScreen(this));
            screens.put(WORLD_SETTINGS_SCREEN_NAME, new WorldSettingsScreen(this));
        }
        initializeWindow(false);
        setupKeyBindings();
        initTimeUpdater();
    }
    private void initializeWindow(boolean preserveState) {
       try {
           GameScreen previousScreen = currentScreen;
           String activeScreenName = getActiveScreenName();
           MetroLogger.logInfo("Screen mode: " + (isFullscreen ? "Fullscreen" : "Windowed"));
           dispose();

           if (isFullscreen) {
               setupFullscreenWindow();
           } else {
               setupWindowedMode();
           }

           initUI();

           if (preserveState && previousScreen != null) {
               screens.put(activeScreenName, previousScreen);
               switchScreen(activeScreenName);
           } else {
               switchScreen(MAIN_MENU_SCREEN_NAME);
           }

           setVisible(true);
       } catch (Exception e) {
           MetroLogger.logError( "Failed changed screen mode!", e);
        }
    }

    private String getActiveScreenName() {
        for (Map.Entry<String, GameScreen> entry : screens.entrySet()) {
            if (entry.getValue() == currentScreen) {
                return entry.getKey();
            }
        }
        return MAIN_MENU_SCREEN_NAME;
    }
    private void setupKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullscreen");
        actionMap.put("toggleFullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFullscreen();
            }
        });
    }
    public void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        initializeWindow(true);
    }
    /**
     * Set to fullscreen mode
     */
    private void setupFullscreenWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true); // Убираем стандартную рамку - на время отладки.
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        isFullscreen = true;
    }
    /**
     * Set to windowed mode
     */
    private void setupWindowedMode() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(false); // Стандартная рамка окна
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));
        setSize(800, 600);
        setLocationRelativeTo(null); // Центрируем окно
        isFullscreen = false;
    }
    /**
     * UI Initialization
     */

    private JWindow currentPopup;
    private AWTEventListener outsideClickListener;
    private void initUI() {

        // Timebar (bottom panel)
        timePanel.removeAll();
            timePanel.setBackground(new Color(60, 60, 60));
            timeLabel = new JLabel(currentTimeString);
            timeLabel.setForeground(Color.WHITE);
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            timePanel.add(timeLabel);
            timePanel.add(StyleUtil.createMetrolineInGameButton("Pause", e -> timeControl()));
            add(timePanel, BorderLayout.SOUTH); // Размещаем внизу окна

        // Toolbar (upper panel)
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setVisible(false);
        toolBar.setBackground(new Color(45, 45, 45));

        toolBar.add(StyleUtil.createMetrolineInGameButton("Save Game", e -> saveGame()));
        toolBar.add(StyleUtil.createMetrolineInGameButton("Load Game", e -> loadGame()));
        toolBar.add(StyleUtil.createMetrolineInGameButton("Back to Menu", e -> backToMenu()));
        JButton menuButton = StyleUtil.createMetrolineInGameButton("Options", e -> showPopupMenu((JButton)e.getSource()));
        toolBar.add(menuButton);
        toolBar.add(StyleUtil.createMetrolineInGameButton("Exit Game", e -> exitGame()));




        add(toolBar, BorderLayout.NORTH);

    }

    private void showPopupMenu(JButton sourceButton) {

        closePopupMenu();
        JPanel menuPanel = new JPanel(new GridLayout(0, 1));
        menuPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JButton option1 = StyleUtil.createMetrolineInGameButton("Option 1", e -> {});
        JButton option2 = StyleUtil.createMetrolineInGameButton("Option 2", e -> {});
        option2.setSize(100, 100);
        menuPanel.add(option1);
        menuPanel.add(option2);

        // Создаем и настраиваем popup-окно
        currentPopup = new JWindow(this);
        currentPopup.getContentPane().add(menuPanel);
        currentPopup.pack();

        // Позиционируем под кнопкой
        Point location = sourceButton.getLocationOnScreen();
        currentPopup.setLocation(location.x, location.y + sourceButton.getHeight());
        currentPopup.setVisible(true);

        // Добавляем глобальный слушатель кликов
        outsideClickListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event.getID() == MouseEvent.MOUSE_PRESSED && currentPopup != null) {
                    MouseEvent mouseEvent = (MouseEvent)event;

                    // Проверяем, был ли клик вне popup-меню
                    if (!currentPopup.getBounds().contains(mouseEvent.getLocationOnScreen()) &&
                            !sourceButton.getBounds().contains(mouseEvent.getPoint())) {
                        closePopupMenu();
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void closePopupMenu() {
        if (currentPopup != null) {
            currentPopup.dispose();
            currentPopup = null;
        }
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }
    }
    /**
     * Time controller
     */
    public void timeControl() {
        if (currentScreen instanceof WorldSandboxScreen) {
            SandboxWorld sandboxWorld = ((WorldSandboxScreen)currentScreen).sandboxWorld;
            if (sandboxWorld != null) {
                sandboxWorld.getGameTime().togglePause();
            }
        }
    }
    /**
     * Back to Main menu
     */
    public void backToMenu() {
        switchScreen(MAIN_MENU_SCREEN_NAME);
    }
    /**
     * Exit from game
     */
    public void exitGame() {
        System.exit(0);
    }

    /**
     * Reset timer
     */
    private void updateTime() {
        if (currentScreen instanceof WorldSandboxScreen) {
            SandboxWorld sandboxWorld = ((WorldSandboxScreen)currentScreen).sandboxWorld;
            if (sandboxWorld != null && sandboxWorld.getGameTime() != null) {
                currentTimeString = sandboxWorld.getGameTime().getDateTimeString();
                timeLabel.setText(currentTimeString);
            }
        }
    }
    private void initTimeUpdater() {
        Timer timer = new Timer(1000, e -> updateTime()); // Обновление каждую секунду
        timer.start();
    }




    /**
     * Save game
     */
    private void saveGame() {
        if (currentScreen instanceof WorldSandboxScreen) {
            ((WorldSandboxScreen)currentScreen).sandboxWorld.saveWorld();
        }
        if (currentScreen instanceof WorldGameScreen) {
            ((WorldGameScreen)currentScreen).gameWorld.saveWorld();
        }
    }

    /**
     * Load game
     */
    private void loadGame() {
        if (currentScreen instanceof WorldSandboxScreen) {
            ((WorldSandboxScreen)currentScreen).sandboxWorld.loadWorld();
        }
        if (currentScreen instanceof WorldGameScreen) {
            ((WorldGameScreen)currentScreen).gameWorld.loadWorld();
        }
    }

    /**
     * Screen switcher
     */
    public void switchScreen(String screenName) {
        if (currentScreen != null) {
            remove(currentScreen);
        }


        boolean isSandboxGameScreen = SANDBOX_SCREEN_NAME.equals(screenName);
        boolean isGameScreen = GAME_SCREEN_NAME.equals(screenName);
        currentScreen = screens.get(screenName);
        add(currentScreen, BorderLayout.CENTER);
        toolBar.setVisible(isGameScreen || isSandboxGameScreen);
        timePanel.setVisible(isGameScreen|| isSandboxGameScreen);
        revalidate();
        repaint();
        currentScreen.requestFocusInWindow();

    }

    /**
     * Get current GameScreen
     * @return Current screen (SUDDENLY!)
     */
    public GameScreen getCurrentScreen() { return currentScreen; }
}
