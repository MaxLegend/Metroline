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
            screens.put("menu", new MenuScreen(this));
            screens.put("game", new WorldSandboxScreen(this));
            screens.put("world_settings", new WorldSettingsScreen(this));
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
               switchScreen("menu");
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
        return "menu";
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
    private void initUI() {

        // Timebar (bottom panel)
        timePanel.removeAll();
            timePanel.setBackground(new Color(60, 60, 60));
            timeLabel = new JLabel(currentTimeString);
            timeLabel.setForeground(Color.WHITE);
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            timePanel.add(timeLabel);
            timePanel.add(StyleUtil.createMetrolineButton("Pause", e -> timeControl()));
            add(timePanel, BorderLayout.SOUTH); // Размещаем внизу окна

        // Toolbar (upper panel)
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setVisible(false);
        toolBar.setBackground(new Color(45, 45, 45));

        toolBar.add(StyleUtil.createMetrolineButton("Save Game", e -> saveGame()));
        toolBar.add(StyleUtil.createMetrolineButton("Load Game", e -> loadGame()));
        toolBar.add(StyleUtil.createMetrolineButton("Back to Menu", e -> backToMenu()));
        toolBar.add(StyleUtil.createMetrolineButton("Exit Game", e -> exitGame()));
        add(toolBar, BorderLayout.NORTH);

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
        switchScreen("menu");
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
    }

    /**
     * Load game
     */
    private void loadGame() {
        if (currentScreen instanceof WorldSandboxScreen) {
            ((WorldSandboxScreen)currentScreen).sandboxWorld.loadWorld();
        }
    }

    /**
     * Screen switcher
     */
    public void switchScreen(String screenName) {
        if (currentScreen != null) {
            remove(currentScreen);
        }
        if(currentScreen instanceof WorldSandboxScreen wgs) {
            wgs.sandboxWorld.getGameTime().reset();
        }
        boolean isGameScreen = "game".equals(screenName);
        currentScreen = screens.get(screenName);
        add(currentScreen, BorderLayout.CENTER);
        toolBar.setVisible("game".equals(screenName));
        timePanel.setVisible(isGameScreen);

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
