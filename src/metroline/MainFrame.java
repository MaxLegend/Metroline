package metroline;


import metroline.input.KeyboardController;
import metroline.objects.gameobjects.GameplayUnits;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.screens.GameScreen;
import metroline.screens.panel.InfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.CachedWorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.MetroLogger;
import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Main application frame that contains all game screens and toolbar
 * TODO: РАЗОБРАТЬСЯ, ПОЧЕМУ ОПЯТЬ КТО ТО ВЫЗЫВАЕТ КАСКАД. НАЙТИ СПОСОБ ВООБЩЕ ЗАПРЕТИТЬ ПЕРЕРИСОВКУ ЭКРАНА
 * @author Tesmio
 */
public class MainFrame extends JFrame {
    public static MainFrame INSTANCE;
    public static final String SANDBOX_SCREEN_NAME = "sandbox_gamescreen";
    public static final String MAIN_MENU_SCREEN_NAME = "menu";
    public static final String GAME_SCREEN_NAME = "gamescreen";
    public static final String WORLD_SETTINGS_SCREEN_NAME = "world_settings";
    public static final String SANDBOX_SETTINGS_SCREEN_NAME = "sandbox_world_settings";
    private boolean repaintBlocked = false;
    private GameScreen currentScreen;
    private String currentScreenName;

    //  private Map<String, GameScreen> screens = new HashMap<>();

    private boolean isFullscreen = false;

    public LinesLegendWindow legendWindow;

    public final ArrayList<ITranslatable> translatables = new ArrayList<>();
    public MainFrameUI mainFrameUI;
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
        INSTANCE = this;
        KeyboardController.initialize(this);
        mainFrameUI = new MainFrameUI(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CursorTooltip.hideTooltip();
            }
        });

        initializeWindow(false);


        updateTranslations();


    }
    public static MainFrame getInstance() {
        return INSTANCE;
    }

    public ArrayList getTranslatables() {
        return translatables;
    }
    public void safeRepaint() {
        if (!repaintBlocked) {
            super.repaint();
        }
    }
    public void setRepaintBlocked(boolean blocked) {
        this.repaintBlocked = blocked;
    }
    private void initializeWindow(boolean preserveState) {
        try {
        String previousScreenName = currentScreenName;
        dispose(); //он тут не причем, все равно изображение теряется

        setSize(1100, 600);
        setLocationRelativeTo(null);

        mainFrameUI.initUI();

        if (preserveState && previousScreenName != null) {
            switchScreen(previousScreenName);
        } else {
            switchScreen(MAIN_MENU_SCREEN_NAME);
        }

        setVisible(true);
    } catch (Exception e) {
        MetroLogger.logError("Failed changed screen mode!", e);
    }
}
    String getActiveScreenName() {
        return currentScreenName;
    }
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
        setSize(1100, 600);
        setLocationRelativeTo(null); // Центрируем окно
        isFullscreen = false;
    }
    public void toggleFullscreen() {
        KeyboardController.getInstance().clearAllKeys();
        setRepaintBlocked(true);

        try {

            if (getCurrentScreen() instanceof CachedWorldScreen) {
                ((CachedWorldScreen) getCurrentScreen()).invalidateCache();
            }
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            if (!isFullscreen) {
                // Переход в полноэкранный режим
                dispose(); // Важно: dispose() перед изменением режима
                setUndecorated(true);

                if (gd.isFullScreenSupported()) {
                    gd.setFullScreenWindow(this);
                } else {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
                if (currentScreen instanceof CachedWorldScreen) {
                    ((CachedWorldScreen) currentScreen).invalidateCache();
                }
                isFullscreen = true;
            } else {

                dispose();
                gd.setFullScreenWindow(null);
                setUndecorated(false);
                setVisible(true);
                if (currentScreen instanceof CachedWorldScreen) {
                    ((CachedWorldScreen) currentScreen).invalidateCache();
                }
                isFullscreen = false;
            }


            revalidate();
            repaint();

            if (currentScreen != null) {
                currentScreen.requestFocusInWindow();
            }
        } finally {
            setRepaintBlocked(false);
            super.repaint(); // один финальный repaint
        }
//        if (isFullscreen) {
//            setupFullscreenWindow();
//        } else {
//            setupWindowedMode();
//        }
}

    public void updateTranslations() {
        for (ITranslatable tc : translatables) {
            tc.updateTranslation();
        }
    }
    public void updateLanguage() {
        LngUtil.setLanguage(LngUtil.getCurrentLanguage());
        updateTranslations();
    }
    public void changeLanguage() {
        if (LngUtil.getCurrentLanguage().equals("ru")) {
            LngUtil.setLanguage("en");

        } else {
            LngUtil.setLanguage("ru");

        }
        updateTranslations();
    }
    public void switchScreen(String screenName) {
        mainFrameUI.switchScreen(screenName);
    }




    /**
     * Get current GameScreen
     * @return Current screen (SUDDENLY!)
     */
    public GameScreen getCurrentScreen() {
        return mainFrameUI.getCurrentScreen();
    }
}
