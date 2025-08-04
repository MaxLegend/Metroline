package screens;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application frame that contains all game screens and toolbar
 */
public class MainFrame extends JFrame {
    private boolean isFullscreen = false;
    private GraphicsDevice device;
    private Dimension windowedSize = new Dimension(1024, 768);
    private Point windowedLocation;

    private GameScreen currentScreen;
    private JToolBar toolBar;
    private Map<String, GameScreen> screens = new HashMap<>();

    static {
        // Устанавливаем настройки Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Основные цвета для темной темы
        Color darkBackground = new Color(45, 45, 45);
        Color darkerBackground = new Color(30, 30, 30);
        Color buttonColor = new Color(60, 60, 60);
        Color borderColor = new Color(80, 80, 80);

        // Настройка основных параметров
        UIManager.put("OptionPane.background", darkBackground);
        UIManager.put("Panel.background", darkBackground);
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("OptionPane.messageFont", new Font("Arial", Font.PLAIN, 14));

        // Стилизация кнопок
        UIManager.put("Button.background", buttonColor);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        UIManager.put("Button.focus", borderColor);
        UIManager.put("Button.font", new Font("Arial", Font.PLAIN, 12));

        // Стилизация текстовых полей
        UIManager.put("TextField.background", darkerBackground);
        UIManager.put("TextField.foreground", Color.WHITE);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(borderColor));
        UIManager.put("TextField.caretForeground", Color.WHITE);

        // Стилизация рамки окна
        UIManager.put("OptionPane.border", BorderFactory.createEmptyBorder(10, 10, 10, 10));
        UIManager.put("OptionPane.buttonAreaBorder", BorderFactory.createEmptyBorder(10, 0, 0, 0));
    }

    public MainFrame() {
        super("Metroline");

        // Получаем графическое устройство
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        device = env.getDefaultScreenDevice();

        // Настройка окна перед отображением
        configureWindow();

        // Инициализация интерфейса
        initComponents();

        // Сохраняем начальную позицию окна
        windowedLocation = getLocation();

        // Включаем полноэкранный режим после всех настроек
        setVisible(true);
        toggleFullscreen(true);
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(windowedSize);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));
        setIgnoreRepaint(true); // Для лучшей производительности

        // Проверяем поддержку полноэкранного режима
        if (!device.isFullScreenSupported()) {
            System.err.println("Полноэкранный режим не поддерживается!");
        }
    }

    private void initComponents() {
        // Инициализация toolbar
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setVisible(false);
        toolBar.setBackground(new Color(45, 45, 45));
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(80, 80, 80)));

        // Кнопки toolbar
        JButton saveButton = createToolbarButton("Save Game");
        JButton loadButton = createToolbarButton("Load Game");

        saveButton.addActionListener(e -> {
            if (currentScreen instanceof WorldGameScreen) {
                ((WorldGameScreen)currentScreen).world.saveWorld();
            }
        });

        loadButton.addActionListener(e -> {
            if (currentScreen instanceof WorldGameScreen) {
                ((WorldGameScreen)currentScreen).world.loadWorld();
            }
        });

        toolBar.add(saveButton);
        toolBar.add(loadButton);
        add(toolBar, BorderLayout.NORTH);

        // Инициализация экранов
        screens.put("menu", new MenuScreen(this));
        screens.put("game", new WorldGameScreen(this));

        // Установка начального экрана
        switchScreen("menu");

        // Настройка горячих клавиш
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        // Переключение полноэкранного режима по F11
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullscreen");

        getRootPane().getActionMap().put("toggleFullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                safeToggleFullscreen(!isFullscreen);
            }
        });
    }

    private void safeToggleFullscreen(boolean fullscreen) {
        try {
            // Сначала скрываем окно для безопасного изменения
            setVisible(false);

            if (fullscreen) {
                // Сохраняем текущие параметры
                if (!isFullscreen) {
                    windowedSize = getSize();
                    windowedLocation = getLocation();
                }

                setUndecorated(true);
                device.setFullScreenWindow(this);
            } else {
                // Возвращаем оконный режим
                setUndecorated(false);
                device.setFullScreenWindow(null);
                setSize(windowedSize);
                setLocation(windowedLocation);
            }

            isFullscreen = fullscreen;
            setVisible(true);
            revalidate();
            repaint();

        } catch (Exception e) {
            System.err.println("!!! ОШИБКА переключения режима !!!");
            e.printStackTrace();

            // Дополнительная диагностика
            System.err.println("Текущее состояние:");
            System.err.println("isFullscreen: " + isFullscreen);
            System.err.println("Размер окна: " + getSize());
            System.err.println("Позиция окна: " + getLocation());
            System.err.println("isDisplayable: " + isDisplayable());
            System.err.println("isShowing: " + isShowing());

            // Альтернативный вариант - максимизированное окно
            System.err.println("Пытаемся восстановить в maximized режиме...");
            try {
                //     setUndecorated(false);
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                setVisible(true);
            } catch (Exception ex) {
                System.err.println("Ошибка при восстановлении:");
                ex.printStackTrace();
            }
        }
        System.out.println("Проверка состояния компонентов:");
        System.out.println("Текущий экран: " + currentScreen);
        System.out.println("Toolbar visible: " + toolBar.isVisible());
        System.out.println("ContentPane children: " + getContentPane().getComponents().length);

    }

    public void toggleFullscreen(boolean fullscreen) {
        safeToggleFullscreen(fullscreen);
    }

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        // Эффекты при наведении
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 60, 60));
            }
        });

        return button;
    }
    public GameScreen getCurrentScreen() {
        return currentScreen;
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
