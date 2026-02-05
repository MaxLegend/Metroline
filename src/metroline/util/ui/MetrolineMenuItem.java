package metroline.util.ui;

import metroline.util.serialize.GlobalSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static metroline.MainFrame.SOUND_ENGINE;

/**
 * Стилизованный пункт меню в стиле Metroline
 */
/**
 * Стилизованный пункт меню в стиле Metroline
 */
public class MetrolineMenuItem extends JMenuItem {
    private String tooltipText;
    public static final Color DEFAULT_BACKGROUND = new Color(60, 60, 60);
    public static final Color HOVER_BACKGROUND = new Color(80, 80, 80);
    public static final Color PRESSED_BACKGROUND = new Color(79, 155, 155);
    public static final Color TEXT_COLOR = Color.WHITE;

    public MetrolineMenuItem(String text) {
        super(text);
        initStyle();
        initEffects();
    }
    public void setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
    }

    public String getTooltipText() {
        return tooltipText;
    }
    public MetrolineMenuItem(String text, Runnable action) {
        this(text);
        addActionListener(e -> action.run());
    }

    public MetrolineMenuItem(String text, ActionListener action) {
        this(text);
        addActionListener(action);
    }

    private void initStyle() {
        setBackground(DEFAULT_BACKGROUND);
        setForeground(TEXT_COLOR); // Явно устанавливаем белый цвет
        setFont(new Font("Sans Serif", Font.BOLD, 13));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        setFocusPainted(false);
        setContentAreaFilled(true); // Изменено на true
        setOpaque(true);

        // Убираем стандартные отступы
        setMargin(new Insets(0, 0, 0, 0));

    }

    private void initEffects() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HOVER_BACKGROUND);
                setForeground(TEXT_COLOR);
                SOUND_ENGINE.playUISound("mouse", 1f);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(DEFAULT_BACKGROUND);
                setForeground(TEXT_COLOR);

                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(PRESSED_BACKGROUND);
                setForeground(TEXT_COLOR);
                SOUND_ENGINE.playUISound("click", GlobalSettings.getSfxVolume());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(HOVER_BACKGROUND);
                setForeground(TEXT_COLOR);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        setForeground(TEXT_COLOR);

        // Кастомная отрисовка фона
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isArmed() || getModel().isPressed()) {
            g2d.setColor(PRESSED_BACKGROUND);
        } else if (getModel().isRollover()) {
            g2d.setColor(HOVER_BACKGROUND);
        } else {
            g2d.setColor(getBackground());
        }

        g2d.fillRect(0, 0, getWidth(), getHeight());


        // Рисуем текст с правильным выравниванием
        g2d.setColor(getForeground());
        g2d.setFont(getFont());

        FontMetrics fm = g2d.getFontMetrics();
        String text = getText();
        int textWidth = fm.stringWidth(text);
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

        g2d.drawString(text, x, y);

    }
}