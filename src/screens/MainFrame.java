package screens;


import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application frame that contains all game screens and toolbar
 */
public class MainFrame extends JFrame {
    private GameScreen currentScreen;
    private JToolBar toolBar;
    private Map<String, GameScreen> screens = new HashMap<>();

    public MainFrame() {
        super("Metroline");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLayout(new BorderLayout());

        // Initialize toolbar (will be visible only in game screen)
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setVisible(false);
        add(toolBar, BorderLayout.NORTH);

        // Create screens
        screens.put("menu", new MenuScreen(this));
        screens.put("game", new WorldGameScreen(this));


        // Set initial screen
        switchScreen("menu");

        setVisible(true);
    }

    /**
     * Switches between different game screens
     * @param screenName Name of the screen to switch to
     */
    public void switchScreen(String screenName) {
        if (currentScreen != null) {
            remove(currentScreen);
        }

        currentScreen = screens.get(screenName);
        add(currentScreen, BorderLayout.CENTER);

        // Show toolbar only in game screen
        toolBar.setVisible("game".equals(screenName));

        revalidate();
        repaint();
        currentScreen.requestFocusInWindow();
    }

    /**
     * Adds a button to the toolbar
     * @param button Button to add
     */
    public void addToolbarButton(JButton button) {
        toolBar.add(button);
    }

}
