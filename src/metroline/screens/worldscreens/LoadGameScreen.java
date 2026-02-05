package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.input.KeyboardController;
import metroline.screens.GameScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.ImageUtil;
import metroline.util.MetroLogger;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Screen for selecting and loading saved games
 */
public class LoadGameScreen extends GameScreen implements CachedBackgroundScreen {

    private Image backgroundImage;
    private boolean backgroundLoaded = false;
    private int bgWidth = 0;
    private int bgHeight = 0;

    private static final String SAVE_FOLDER = "saves";
    private JList<String> savesList;
    private DefaultListModel<String> listModel;

    public LoadGameScreen(MainFrame parent) {
        super(parent);

        // Load random background
        String[] backgroundCandidates = { "bc5.png"};
        for (String imageName : backgroundCandidates) {
            ImageCacheUtil.loadCachedImage("/backgrounds/" + imageName);
        }
        String selectedBackground = backgroundCandidates[new Random().nextInt(backgroundCandidates.length)];
        ImageUtil.loadBackgroundImage(this, selectedBackground);

        initUI(parent);
    }

    private void initUI(MainFrame parent) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Main container panel
        JPanel containerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 30, 220));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.setColor(new Color(70, 70, 70));
                g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2d.dispose();
            }
        };
        containerPanel.setLayout(new BorderLayout(10, 10));
        containerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        containerPanel.setOpaque(false);
        containerPanel.setPreferredSize(new Dimension(800, 450));

        // Title
        MetrolineLabel titleLabel = new MetrolineLabel("loadScreen.load_title", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(MetrolineFont.getMainFont(24));
        containerPanel.add(titleLabel, BorderLayout.NORTH);

        // Saves list
        listModel = new DefaultListModel<>();
        loadSaveFiles();

        savesList = new JList<>(listModel);
        savesList.setBackground(new Color(45, 45, 45));
        savesList.setForeground(Color.WHITE);
        savesList.setSelectionBackground(new Color(70, 100, 150));
        savesList.setSelectionForeground(Color.WHITE);
        savesList.setFont(MetrolineFont.getMainFont(14));
        savesList.setFixedCellHeight(35);
        savesList.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Custom cell renderer
        savesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? new Color(70, 100, 150) : new Color(45, 45, 45));
                setForeground(Color.WHITE);
                setBorder(new EmptyBorder(8, 15, 8, 15));
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(savesList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        scrollPane.getViewport().setBackground(new Color(45, 45, 45));
        containerPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonsPanel.setOpaque(false);

        MetrolineButton loadButton = MetrolineButton.createMetrolineButton(LngUtil.translatable("loadScreen.loadButton"),
                e -> loadSelectedSave(parent));
        loadButton.setPreferredSize(new Dimension(120, 45));
        loadButton.setFont(MetrolineFont.getMainFont(16));

        MetrolineButton deleteButton = MetrolineButton.createMetrolineButton(LngUtil.translatable("loadScreen.delButton"),
                e -> deleteSelectedSave());
        deleteButton.setPreferredSize(new Dimension(120, 45));
        deleteButton.setFont(MetrolineFont.getMainFont(16));

        MetrolineButton backButton = MetrolineButton.createMetrolineButton(LngUtil.translatable("loadScreen.backButton"),
                e -> parent.switchScreen(MainFrame.WORLD_MENU_SCREEN_NAME));
        backButton.setPreferredSize(new Dimension(120, 45));
        backButton.setFont(MetrolineFont.getMainFont(16));

        buttonsPanel.add(backButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(loadButton);
        containerPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Center the container
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(containerPanel, gbc);
    }

    private void loadSaveFiles() {
        listModel.clear();
        File saveDir = new File(SAVE_FOLDER);

        if (saveDir.exists() && saveDir.isDirectory()) {
            File[] files = saveDir.listFiles((dir, name) -> name.endsWith(".metro"));
            if (files != null) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (File file : files) {
                    listModel.addElement(file.getName());
                }
            }
        }

        if (listModel.isEmpty()) {
            listModel.addElement("No saves found");
        }
    }

    private void loadSelectedSave(MainFrame parent) {
        String selected = savesList.getSelectedValue();
        if (selected == null || selected.equals("No saves found")) {
            return;
        }

        MetroLogger.logInfo("[LoadGameScreen::loadSelectedSave] Loading: " + selected);

        // Switch to game screen and load the save
        parent.switchScreen(MainFrame.GAME_SCREEN_NAME);
        GameWorldScreen gameScreen = (GameWorldScreen) parent.getCurrentScreen();
        gameScreen.getWorld().SAVE_FILE = selected;
        gameScreen.getWorld().loadWorld();
//        gameScreen.invalidateCache();
//        gameScreen.repaint();
    }

    private void deleteSelectedSave() {
        String selected = savesList.getSelectedValue();
        if (selected == null || selected.equals("No saves found")) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete save '" + selected + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            File saveFile = new File(SAVE_FOLDER + File.separator + selected);
            if (saveFile.delete()) {
                MetroLogger.logInfo("[LoadGameScreen::deleteSelectedSave] Deleted: " + selected);
                loadSaveFiles();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundLoaded && backgroundImage != null) {
            ImageUtil.drawScaledProportional(g, backgroundImage,
                    bgWidth, bgHeight, getWidth(), getHeight());
        } else {
            g.setColor(new Color(20, 20, 20));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // CachedBackgroundScreen implementation
    @Override
    public void setBackgroundImage(Image image) { this.backgroundImage = image; }
    @Override
    public void setBackgroundLoaded(boolean loaded) { this.backgroundLoaded = loaded; }
    @Override
    public void setBackgroundSize(int width, int height) { this.bgWidth = width; this.bgHeight = height; }
    @Override
    public Image getBackgroundImage() { return backgroundImage; }
    @Override
    public boolean isBackgroundLoaded() { return backgroundLoaded; }
    @Override
    public int getBackgroundWidth() { return bgWidth; }
    @Override
    public int getBackgroundHeight() { return bgHeight; }

    @Override
    public void onActivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(this);
        loadSaveFiles(); // Refresh list when screen becomes active
        requestFocusInWindow();
    }

    @Override
    public void onDeactivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(null);
    }
}
