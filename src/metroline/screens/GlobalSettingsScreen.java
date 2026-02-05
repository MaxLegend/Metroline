package metroline.screens;

import metroline.MainFrame;
import metroline.core.soundengine.SoundEngine;
import metroline.input.KeyboardController;
import metroline.util.localizate.LngUtil;
import metroline.util.serialize.GlobalSettings;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolineLabel;
import metroline.util.ui.MetrolineSlider;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;

public class GlobalSettingsScreen extends GameScreen {

    private MetrolineSlider volumeSlider;
    private MetrolineLabel volumeLabel;
    private MetrolineSlider musicVolumeSlider;
    private MetrolineLabel musicVolumeLabel;

    private MetrolineSlider sfxVolumeSlider;
    private MetrolineLabel sfxVolumeLabel;

    private MetrolineSlider pngScaleSlider;
    private MetrolineLabel pngScaleLabel;

    public GlobalSettingsScreen(MainFrame parent) {
        super(parent);
        setBackground(new Color(30, 30, 30));

        // Создаём центральную панель с вертикальным BoxLayout
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100)); // отступы

        // Title
        MetrolineLabel title = new MetrolineLabel("global_settings.title", SwingUtilities.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 42));
        title.setAlignmentX(Component.CENTER_ALIGNMENT); // важно для BoxLayout
        centerPanel.add(title);
        centerPanel.add(Box.createVerticalStrut(40)); // отступ

        // Volume Slider Panel
        JPanel volumePanel = new JPanel(new BorderLayout(10, 0));
        volumePanel.setOpaque(false);
        volumePanel.setMaximumSize(new Dimension(400, 40)); // ограничиваем ширину

        MetrolineLabel volumeTitle = new MetrolineLabel("global_settings.volume");
        volumeTitle.setForeground(Color.WHITE);
        volumeTitle.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        volumeLabel = new MetrolineLabel("100%");
        volumeLabel.setForeground(Color.WHITE);
        volumeLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        volumeSlider = new MetrolineSlider(

                0.0f,
                1.0f,
                GlobalSettings.getVolume(),
                0.1f
        );
        volumeSlider.setValueLabel(volumeLabel, "%");

        volumePanel.add(volumeTitle, BorderLayout.WEST);
        volumePanel.add(volumeSlider, BorderLayout.CENTER);
        volumePanel.add(volumeLabel, BorderLayout.EAST);

        volumePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(volumePanel);
        centerPanel.add(Box.createVerticalStrut(30)); // отступ
        JPanel musicVolumePanel = new JPanel(new BorderLayout(10, 0));
        musicVolumePanel.setOpaque(false);
        musicVolumePanel.setMaximumSize(new Dimension(400, 40));

        MetrolineLabel musicVolumeTitle = new MetrolineLabel("global_settings.music_volume");
        musicVolumeTitle.setForeground(Color.WHITE);
        musicVolumeTitle.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        musicVolumeLabel = new MetrolineLabel("100%");
        musicVolumeLabel.setForeground(Color.WHITE);
        musicVolumeLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        musicVolumeSlider = new MetrolineSlider(

                0.0f,
                1.0f,
                GlobalSettings.getMusicVolume(), // <-- Загружаем сохранённое значение
                0.1f
        );
        musicVolumeSlider.setValueLabel(musicVolumeLabel, "%");

        musicVolumePanel.add(musicVolumeTitle, BorderLayout.WEST);
        musicVolumePanel.add(musicVolumeSlider, BorderLayout.CENTER);
        musicVolumePanel.add(musicVolumeLabel, BorderLayout.EAST);
        musicVolumePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(musicVolumePanel);
        centerPanel.add(Box.createVerticalStrut(20));

// Добавляем SFX Volume Slider
        JPanel sfxVolumePanel = new JPanel(new BorderLayout(10, 0));
        sfxVolumePanel.setOpaque(false);
        sfxVolumePanel.setMaximumSize(new Dimension(400, 40));

        MetrolineLabel sfxVolumeTitle = new MetrolineLabel("global_settings.sfx_volume");
        sfxVolumeTitle.setForeground(Color.WHITE);
        sfxVolumeTitle.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        sfxVolumeLabel = new MetrolineLabel("100%");
        sfxVolumeLabel.setForeground(Color.WHITE);
        sfxVolumeLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        sfxVolumeSlider = new MetrolineSlider(
                0.0f,
                1.0f,
                GlobalSettings.getSfxVolume(), // <-- Загружаем сохранённое значение
                0.1f
        );
        sfxVolumeSlider.setValueLabel(sfxVolumeLabel, "%");

        sfxVolumePanel.add(sfxVolumeTitle, BorderLayout.WEST);
        sfxVolumePanel.add(sfxVolumeSlider, BorderLayout.CENTER);
        sfxVolumePanel.add(sfxVolumeLabel, BorderLayout.EAST);
        sfxVolumePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(sfxVolumePanel);
        centerPanel.add(Box.createVerticalStrut(30));

        // PNG Scale Slider Panel
        JPanel pngScalePanel = new JPanel(new BorderLayout(10, 0));
        pngScalePanel.setOpaque(false);
        pngScalePanel.setMaximumSize(new Dimension(400, 40));

        MetrolineLabel pngScaleTitle = new MetrolineLabel("global_settings.png_scale");
        pngScaleTitle.setForeground(Color.WHITE);
        pngScaleTitle.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        pngScaleLabel = new MetrolineLabel(GlobalSettings.getPngScale() + "x");
        pngScaleLabel.setForeground(Color.WHITE);
        pngScaleLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));

        pngScaleSlider = new MetrolineSlider(LngUtil.translatable("global_settings.png_scale_desc"),1.0f, 4.0f, (float) GlobalSettings.getPngScale(), 1.0f);
        pngScaleSlider.addChangeListener(e -> {
            int scale = Math.round(pngScaleSlider.getValue());
            pngScaleLabel.setText(scale + "x");
            GlobalSettings.setPngScale(scale);
        });

        pngScalePanel.add(pngScaleTitle, BorderLayout.WEST);
        pngScalePanel.add(pngScaleSlider, BorderLayout.CENTER);
        pngScalePanel.add(pngScaleLabel, BorderLayout.EAST);
        pngScalePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(pngScalePanel);
        centerPanel.add(Box.createVerticalStrut(30));

        // Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        MetrolineButton changeLngButton = MetrolineButton.createMetrolineButton("global_settings.change_lng", e -> parent.changeLanguage());
        MetrolineButton exitButton = MetrolineButton.createMetrolineButton("button.backToMenu", e -> parent.mainFrameUI.backToMenu());

        changeLngButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel.add(changeLngButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(exitButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(buttonPanel);

        // Добавляем слушатели для перевода
        parent.translatables.add(title);
        parent.translatables.add(changeLngButton);
        parent.translatables.add(exitButton);
        parent.translatables.add(musicVolumeTitle);
        parent.translatables.add(sfxVolumeTitle);
        parent.translatables.add(volumeTitle);
        parent.translatables.add(pngScaleTitle);
        // Подписываемся на изменение громкости
        volumeSlider.addChangeListener(e -> {
            float volume = volumeSlider.getValue();
            try {
                updateGlobalVolume(volume);
            } catch (LineUnavailableException ex) {
                throw new RuntimeException(ex);
            }
        });
        musicVolumeSlider.addChangeListener(e -> {
            float volume = musicVolumeSlider.getValue();
            try {
                SoundEngine.getInstance().setMusicVolume(volume);
                GlobalSettings.setMusicVolume(volume); // сохраняем
            } catch (LineUnavailableException ex) {
                throw new RuntimeException(ex);
            }
        });

        sfxVolumeSlider.addChangeListener(e -> {
            float volume = sfxVolumeSlider.getValue();
            try {
                SoundEngine.getInstance().setSfxVolume(volume);
                GlobalSettings.setSfxVolume(volume); // сохраняем
            } catch (LineUnavailableException ex) {
                throw new RuntimeException(ex);
            }
        });
        // Центрируем всю панель в родительском контейнере
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(centerPanel, gbc);

        parent.updateLanguage();
    }

    private void updateGlobalVolume(float volume) throws LineUnavailableException {
        SoundEngine soundEngine = SoundEngine.getInstance();
        soundEngine.setGlobalVolume(volume);           // обновляем глобальный множитель
        soundEngine.updateMusicVolume(1.0f);           // обновляем громкость текущей музыки (1.0f — потому что множитель уже учтён)

        // Сохраняем в файл
        GlobalSettings.setVolume(volume);}


    @Override
    public void onActivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(this);
        requestFocusInWindow();
    }

    @Override
    public void onDeactivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(null);
    }
}