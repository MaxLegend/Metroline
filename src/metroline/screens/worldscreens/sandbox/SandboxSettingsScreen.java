package metroline.screens.worldscreens.sandbox;

import metroline.MainFrame;
import metroline.screens.GameScreen;
import metroline.util.ColorUtil;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SandboxSettingsScreen extends GameScreen {
    public static final Color[] WORLD_COLORS = {
            new Color(203, 202, 202), // Светло-серый
            new Color(110, 110, 110), // Серый (по умолчанию)
            new Color(70, 70, 70),    // Темно-серый
            new Color(150, 100, 80),  // Коричневый
            new Color(80, 100, 150)   // Голубоватый
    };


    private MetrolineSlider widthSlider;
    private MetrolineSlider heightSlider;

    private MetrolineCheckbox roundStationsCheck;

    private MainFrame parent;
    private JButton colorButton; // Кнопка для выбора цвета
    private int worldColor = 0x6E6E6E; // Цвет по умолчанию


    public SandboxSettingsScreen(MainFrame parent) {
        super(parent);
        this.parent = parent;

        setBackground(StyleUtil.BACKGROUND_COLOR);
        setLayout(new BorderLayout(0, 0)); // Changed to BorderLayout with horizontal gap
        initUI();
    }


    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 1.0;

        // Заголовок
        JLabel title = StyleUtil.createMetrolineLabel(
                LngUtil.translatable("create_world_title"),
                SwingConstants.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(title, gbc);

        // Слайдеры размеров
        widthSlider = createWorldSizeSlider(
                "world.width",
                "width_world_slider_desc",
                20, 200, 40
        );
        mainPanel.add(createSliderPanel(widthSlider), gbc);

        heightSlider = createWorldSizeSlider(
                "world.height",
                "height_world_slider_desc",
                20, 200, 40
        );
        mainPanel.add(createSliderPanel(heightSlider), gbc);

        // Настройки внешнего вида
        roundStationsCheck = StyleUtil.createMetrolineCheckBox("world.round_stations",
                "world.round_stations_desc"
        );
        mainPanel.add(roundStationsCheck, gbc);

        colorButton = createColorButton();
        mainPanel.add(colorButton, gbc);

        // Кнопки действий
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(StyleUtil.BACKGROUND_COLOR);

        buttonPanel.add(MetrolineButton.createMetrolineButton(
                LngUtil.translatable("world.back"),
                e -> parent.switchScreen("menu")
        ));
        buttonPanel.add(MetrolineButton.createMetrolineButton(
                LngUtil.translatable("world.create_sandbox"),
                e -> createSandboxWorld()
        ));

        gbc.insets = new Insets(30, 0, 0, 0);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private MetrolineSlider createWorldSizeSlider(String labelKey, String descKey,
            int min, int max, int defaultValue) {
        MetrolineSlider slider = new MetrolineSlider(
                LngUtil.translatable(descKey),
                min, max, defaultValue, 10
        );
        slider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return slider;
    }

    private JPanel createSliderPanel(MetrolineSlider slider) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(StyleUtil.BACKGROUND_COLOR);
        panel.setMaximumSize(new Dimension(400, 50));

        MetrolineLabel label = StyleUtil.createMetrolineLabel(
                LngUtil.translatable(slider == widthSlider ? "world.width" : "world.height"),
                SwingConstants.LEFT
        );
        label.setFont(new Font("Sans Serif", Font.BOLD, 13));

        MetrolineLabel valueLabel = StyleUtil.createMetrolineLabel("", SwingConstants.RIGHT);
        valueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        slider.setValueLabel(valueLabel, " M ");

        panel.add(label, BorderLayout.WEST);
        panel.add(slider, BorderLayout.CENTER);
        panel.add(valueLabel, BorderLayout.EAST);

        return panel;
    }

    private JButton createColorButton() {
        JButton button = MetrolineButton.createMetrolineColorableButton(
                LngUtil.translatable("world.color"),
                e -> showColorSelectionDialog(),
                new Color(worldColor)
        );

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(StyleUtil.changeColorShade(new Color(worldColor), 20));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(worldColor));
            }
        });

        return button;
    }

    private void showColorSelectionDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel colorPanel = new JPanel(new GridLayout(1, WORLD_COLORS.length, 5, 0));
        colorPanel.setBorder(BorderFactory.createLineBorder(StyleUtil.FOREGROUND_COLOR, 2));
        colorPanel.setBackground(StyleUtil.BACKGROUND_COLOR);

        for (Color color : WORLD_COLORS) {
            JButton colorBtn = createColorSelectionButton(color, dialog);
            colorPanel.add(colorBtn);
        }

        dialog.add(colorPanel);
        dialog.pack();
        positionDialogNearButton(dialog, colorButton);
        dialog.setVisible(true);
    }

    private JButton createColorSelectionButton(Color color, JDialog parentDialog) {
        JButton button = new JButton();
        button.setBackground(color);
        button.setPreferredSize(new Dimension(40, 40));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setOpaque(true);
        button.setFocusPainted(false);

        button.addActionListener(e -> {
            worldColor = ColorUtil.colorToRGB(color);
            colorButton.setBackground(color);
            parentDialog.dispose();
        });

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(StyleUtil.changeColorShade(color, -40));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void positionDialogNearButton(JDialog dialog, JButton button) {
        Point buttonLoc = button.getLocationOnScreen();
        dialog.setLocation(
                buttonLoc.x + button.getWidth()/2 - dialog.getWidth()/2,
                buttonLoc.y + button.getHeight() + 5
        );
        dialog.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent e) {
                dialog.dispose();
            }
        });
    }

    private void createSandboxWorld() {
        short width = (short) widthSlider.getValue();
        short height = (short) heightSlider.getValue();
        boolean roundStations = roundStationsCheck.isSelected();

        parent.switchScreen(MainFrame.SANDBOX_SCREEN_NAME);
        SandboxWorldScreen gameScreen = (SandboxWorldScreen) parent.getCurrentScreen();
        gameScreen.createNewWorld(width, height, worldColor);
        gameScreen.getWorld().setRoundStationsEnabled(roundStations);
    }

    @Override
    public void onActivate() {
        widthSlider.setValue(100);
        heightSlider.setValue(100);
        roundStationsCheck.setSelected(false);
    }

    @Override
    public void onDeactivate() {
        // Очистка ресурсов при необходимости
    }
}