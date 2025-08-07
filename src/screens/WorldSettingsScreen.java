package screens;

import util.StyleUtil;

import javax.swing.*;
import java.awt.*;

public class WorldSettingsScreen extends GameScreen {
    private JSlider widthSlider;
    private JSlider heightSlider;
    private JCheckBox organicPatchesCheck;
    private JCheckBox riversCheck;
    private MainFrame parent;



    public WorldSettingsScreen(MainFrame parent) {
        super(parent);
        this.parent = parent;
        setBackground(StyleUtil.BACKGROUND_COLOR);
        setLayout(new GridBagLayout());
        initUI();
    }

    private void initUI() {
        // Основной контейнер для центрирования
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 30, 10, 30);
        gbc.weightx = 1;

        // Title
        JLabel title = new JLabel("CREATE NEW WORLD", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(StyleUtil.FOREGROUND_COLOR);
        gbc.insets = new Insets(0, 30, 30, 30);
        centerPanel.add(title, gbc);

        // Reset insets for other components
        gbc.insets = new Insets(10, 30, 10, 30);

        // World Size
        JPanel sizePanel = new JPanel(new GridLayout(2, 2, 10, 15));
        sizePanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        sizePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        widthSlider = StyleUtil.createMetrolineSlider(50, 200, 100, "Width: ");
        heightSlider = StyleUtil.createMetrolineSlider(50, 200, 100, "Height: ");

        JLabel widthLabel = StyleUtil.createMetrolineLabel("World Width:");
        JLabel heightLabel = StyleUtil.createMetrolineLabel("World Height:");

        sizePanel.add(widthLabel);
        sizePanel.add(widthSlider);
        sizePanel.add(heightLabel);
        sizePanel.add(heightSlider);

        centerPanel.add(sizePanel, gbc);

        // World Features
        JPanel featuresPanel = new JPanel(new GridLayout(2, 1, 5, 10));
        featuresPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        featuresPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        organicPatchesCheck = StyleUtil.createMetrolineCheckBox("Generate Organic Patches", true);
        riversCheck = StyleUtil.createMetrolineCheckBox("Generate Rivers", true);

        featuresPanel.add(organicPatchesCheck);
        featuresPanel.add(riversCheck);

        centerPanel.add(featuresPanel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JButton createButton = StyleUtil.createMetrolineButton("Create World",e -> createWorld());


        JButton backButton = StyleUtil.createMetrolineButton("Back",e -> parent.switchScreen("menu"));

        buttonPanel.add(backButton);
        buttonPanel.add(createButton);

        centerPanel.add(buttonPanel, gbc);

        // Добавляем центральную панель в основной экран
        add(centerPanel);
    }





    private void createWorld() {
        int width = widthSlider.getValue();
        int height = heightSlider.getValue();
        boolean hasOrganicPatches = organicPatchesCheck.isSelected();
        boolean hasRivers = riversCheck.isSelected();

        parent.switchScreen("game");
        WorldSandboxScreen gameScreen = (WorldSandboxScreen) parent.getCurrentScreen();
        gameScreen.createNewWorld(width, height, hasOrganicPatches, hasRivers);
    }

    @Override
    public void onActivate() {
        widthSlider.setValue(100);
        heightSlider.setValue(100);
        organicPatchesCheck.setSelected(true);
        riversCheck.setSelected(true);
    }

    // Кастомный UI для слайдера

}