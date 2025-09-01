package metroline;


import metroline.input.KeyboardController;
import metroline.screens.GameScreen;
import metroline.screens.panel.LinesLegendWindow;
import metroline.util.MetroLogger;

import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.ImageCacheUtil;
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
    public static final String WORLD_MENU_SCREEN_NAME = "world_menu";
    public static final String GAME_WORLD_SETTINGS_SCREEN_NAME = "game_world_settings";
    public static final String SANDBOX_SETTINGS_SCREEN_NAME = "sandbox_world_settings";
    public static final String GLOBAL_SETTINGS_SCREEN_NAME = "global_settings";

    private boolean repaintBlocked = false;
    private GameScreen currentScreen;
    private String currentScreenName;


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
    public void preloadBackgrounds() {
        // Предзагружаем все фоновые изображения при запуске
        String[] backgrounds = {
                "/backgrounds/background.png",
                "/backgrounds/background2.png",
                // добавьте другие фоны которые используются
        };

        for (String bg : backgrounds) {
            ImageCacheUtil.loadCachedImage(bg);
        }
    }
    /**
     * MainFrame constructor
     */
    public MainFrame() {
        super("Metroline");
        INSTANCE = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        KeyboardController.initialize(this);

        mainFrameUI = new MainFrameUI(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CursorTooltip.hideTooltip();
            }
        });
        preloadBackgrounds();
        initializeWindow(false);


        updateTranslations();


    }


    public static MainFrame getInstance() {
        return INSTANCE;
    }

    public ArrayList getTranslatables() {
        return translatables;
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

public void toggleFullscreen() {
    try {
        if (!isFullscreen) {
            // Переход в "полноэкранный" режим (на самом деле просто максимизированное окно)
            dispose();
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setUndecorated(true);
            setVisible(true);
            isFullscreen = true;
        } else {
            // Возврат в оконный режим
            dispose();
            setUndecorated(false);
            setExtendedState(JFrame.NORMAL);
            setSize(1100, 600);
            setLocationRelativeTo(null);
            setVisible(true);
            isFullscreen = false;
        }


    } catch (Exception e) {
        MetroLogger.logError("Failed to toggle fullscreen", e);

        // Аварийное восстановление
        try {
            dispose();
            setUndecorated(false);
            setExtendedState(JFrame.NORMAL);
            setSize(1100, 600);
            setLocationRelativeTo(null);
            setVisible(true);
            isFullscreen = false;
        } catch (Exception ex) {
            MetroLogger.logError("Critical: failed to recover window state", ex);
        }
    }
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


    public void setCurrentScreen(GameScreen screen) {
        this.currentScreen = screen;
    }

    /**
     * Get current GameScreen
     * @return Current screen (SUDDENLY!)
     */
    public GameScreen getCurrentScreen() {

        return mainFrameUI.getCurrentScreen();
    }
}
