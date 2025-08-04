package screens;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application frame that contains all game screens and toolbar
 */
/**
 * Главное окно приложения с кастомным оформлением и полноэкранным режимом
 * TODO: прекращает работать сохранение картинок или загрузка если что то уже было выполнено в мире
 */
public class MainFrame extends JFrame {
    // Основные компоненты интерфейса
    private GameScreen currentScreen;
    private JToolBar toolBar;
    private Map<String, GameScreen> screens = new HashMap<>();

    // Настройки стиля
    static {
        try {
            // Устанавливаем стандартный Look and Feel
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

            // Темная цветовая схема
            Color darkBg = new Color(45, 45, 45);
            UIManager.put("OptionPane.background", darkBg);
            UIManager.put("Panel.background", darkBg);
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Конструктор главного окна
     */
    public MainFrame() {
        super("Metroline");
        setupWindow();
        initUI();
    }

    /**
     * Настройка параметров окна
     */
    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true); // Убираем стандартную рамку
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));

        // Разворачиваем на весь экран
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Инициализация пользовательского интерфейса
     */
    private void initUI() {
        // Панель инструментов
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setVisible(false);
        toolBar.setBackground(new Color(45, 45, 45));

        // Кнопки сохранения/загрузки
        toolBar.add(createToolButton("Save Game", e -> saveGame()));
        toolBar.add(createToolButton("Load Game", e -> loadGame()));
        toolBar.add(createToolButton("Back to Menu", e -> backToMenu()));
        toolBar.add(createToolButton("Exit Game", e -> exitGame()));
        add(toolBar, BorderLayout.NORTH);

        // Инициализация экранов
        screens.put("menu", new MenuScreen(this));
        screens.put("game", new WorldGameScreen(this));
        switchScreen("menu");
    }
    public void backToMenu() {
        switchScreen("menu");
    }
    public void exitGame() {
        System.exit(0);
    }

    /**
     * Создает кнопку для панели управления
     */
    private JButton createControlButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setForeground(Color.WHITE);
        button.addActionListener(action);

        // Эффект при наведении
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(new Color(80, 80, 80)); }
            public void mouseExited(MouseEvent e) { button.setBackground(null); }
        });

        return button;
    }

    /**
     * Создает кнопку для панели инструментов
     */
    private JButton createToolButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.addActionListener(action);

        // Эффект при наведении
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(new Color(80, 80, 80)); }
            public void mouseExited(MouseEvent e) { button.setBackground(new Color(60, 60, 60)); }
        });

        return button;
    }

    /**
     * Сохранение игры
     */
    private void saveGame() {
        if (currentScreen instanceof WorldGameScreen) {
            ((WorldGameScreen)currentScreen).world.saveWorld();
        }
    }

    /**
     * Загрузка игры
     */
    private void loadGame() {
        if (currentScreen instanceof WorldGameScreen) {
            ((WorldGameScreen)currentScreen).world.loadWorld();
        }
    }

    /**
     * Переключение между экранами
     */
    public void switchScreen(String screenName) {
        if (currentScreen != null) {
            remove(currentScreen);
        }

        currentScreen = screens.get(screenName);
        add(currentScreen, BorderLayout.CENTER);
        toolBar.setVisible("game".equals(screenName));

        revalidate();
        repaint();
        currentScreen.requestFocusInWindow();
    }

    // Геттеры
    public GameScreen getCurrentScreen() { return currentScreen; }
}
