package metroline.util.ui;

import metroline.MainFrame;
import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class MetrolineToggleButton extends JToggleButton implements ITranslatable {
    public static final Color DEFAULT_BACKGROUND = new Color(60, 60, 60);
    public static final Color HOVER_BACKGROUND = new Color(80, 80, 80);
    public static final Color PRESSED_BACKGROUND = new Color(79, 155, 155);
    public static final Color SELECTED_BACKGROUND = new Color(50, 120, 120);
    public static final Color DEFAULT_FOREGROUND = Color.WHITE;
    private String translationKey;
    public static final Font DEFAULT_FONT = new Font("Sans Serif", Font.BOLD, 13);
    private String tooltipText;
    private String tooltipKey;

    public MetrolineToggleButton(String text) {
        super(text);
        this.translationKey = text;
        initDefaultStyle();
    }
    public MetrolineToggleButton(String text, String tooltipKey) {
        super(text);
        this.tooltipKey = tooltipKey;
        this.translationKey = text;
        this.tooltipText = "";
        initDefaultStyle();
    }
    public MetrolineToggleButton(String text, ActionListener action) {
        super(text);
        this.translationKey = text;
        initDefaultStyle();
        addActionListener(action);
    }


    @Override
    public void setToolTipText(String text) {
        this.tooltipText = text;
    }

    private void initDefaultStyle() {
        setForeground(DEFAULT_FOREGROUND);
        setFont(DEFAULT_FONT);
        setBackground(DEFAULT_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);
        setPreferredSize(new Dimension(35, 30));

        mouseReactions();
    }
    public void mouseReactions() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(HOVER_BACKGROUND);
                }
                // Показываем тултип у курсора
                Point mousePos = e.getPoint();
                SwingUtilities.convertPointToScreen(mousePos, MetrolineToggleButton.this);
                Point framePos = MainFrame.INSTANCE.getLocationOnScreen();
                CursorTooltip.showTooltip(tooltipText,
                        mousePos.x - framePos.x,
                        mousePos.y - framePos.y);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(DEFAULT_BACKGROUND);
                }
                // Скрываем тултип
                CursorTooltip.hideTooltip();
            }

        });

// Добавляем MotionListener для обновления при движении внутри кнопки
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (CursorTooltip.isVisible() &&
                        CursorTooltip.getCurrentText().equals(tooltipText)) {
                    Point mousePos = e.getPoint();
                    SwingUtilities.convertPointToScreen(mousePos, MetrolineToggleButton.this);
                    Point framePos = MainFrame.INSTANCE.getLocationOnScreen();
                    CursorTooltip.showTooltip(tooltipText,
                            mousePos.x - framePos.x,
                            mousePos.y - framePos.y);
                }
            }
        });
        addItemListener(e -> {
            if (isSelected()) {
                setBackground(PRESSED_BACKGROUND);
            } else {
                setBackground(DEFAULT_BACKGROUND);
            }
        });
    }
    @Override
    public void updateTranslation() {
        setText(LngUtil.translatable(translationKey));

        // Translate tooltip key if exists
        if(tooltipKey != null && !tooltipKey.isEmpty()) {
            this.tooltipText = LngUtil.translatable(tooltipKey);
        }
    }
    private void initCustomColorStyle(Color baseColor) {
        setForeground(DEFAULT_FOREGROUND);
        setFont(DEFAULT_FONT);
        setBackground(baseColor);
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);

        Color hoverColor = StyleUtil.changeColorShade(baseColor, 20);
        Color pressedColor = StyleUtil.changeColorShade(baseColor, -20);
        Color selectedColor = StyleUtil.changeColorShade(baseColor, -30);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(baseColor);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(pressedColor);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(hoverColor);
                }
            }
        });

        addItemListener(e -> {
            if (isSelected()) {
                setBackground(selectedColor);
            } else {
                setBackground(baseColor);
            }
        });
    }

    private void initIconStyle() {
        setForeground(DEFAULT_FOREGROUND);
        setFont(StyleUtil.getMetrolineFont(14));
        setBackground(DEFAULT_BACKGROUND);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);
        setMargin(new Insets(0, 5, 0, 0));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(HOVER_BACKGROUND);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected()) {
                    setBackground(DEFAULT_BACKGROUND);
                }
            }
        });

        addItemListener(e -> {
            if (isSelected()) {
                setBackground(SELECTED_BACKGROUND);
            } else {
                setBackground(DEFAULT_BACKGROUND);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем фон с закругленными углами
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

        // Рисуем текст
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        Rectangle stringBounds = fm.getStringBounds(getText(), g2).getBounds();
        int textX = (getWidth() - stringBounds.width) / 2;
        int textY = (getHeight() - stringBounds.height) / 2 + fm.getAscent();
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Убираем стандартную отрисовку границы
    }
}