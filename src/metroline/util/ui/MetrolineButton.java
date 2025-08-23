package metroline.util.ui;

import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MetrolineButton extends JButton implements ITranslatable {
    public static final Color DEFAULT_BACKGROUND = new Color(60, 60, 60);
    public static final Color HOVER_BACKGROUND = new Color(80, 80, 80);
    public static final Color CLICK_BACKGROUND = new Color(79, 155, 155);
    public static final Color DEFAULT_FOREGROUND = Color.WHITE;
    public static final Font DEFAULT_FONT = new Font("Sans Serif", Font.BOLD, 13);

    private String translationKey;

    public MetrolineButton(String text) {
        super(text);
        this.translationKey = text;
        initDefaultStyle();
    }

    public MetrolineButton(String text, ActionListener action) {
        super(text);
        this.translationKey = text;
        initDefaultStyle();
        addActionListener(action);
    }

    public MetrolineButton(String text, Color customColor, ActionListener action) {
        super(text);
        this.translationKey = text;
        initCustomColorStyle(customColor);
        addActionListener(action);
    }

    public MetrolineButton(String iconText, String tooltip, ActionListener action) {
        super(iconText);
        initIconStyle();
        setLocalizedTooltip(tooltip);
        addActionListener(action);
    }
    public static MetrolineButton createMetrolineButton(String text, ActionListener action) {
        MetrolineButton button = new MetrolineButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.setBackground(new Color(50, 50, 50));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Sans Serif", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.addActionListener(action);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {button.setBackground(new Color(80, 80, 80));}
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 60, 60));
            }
        });
        return button;
    }
    public static MetrolineButton createMetrolineColorableButton(String text, ActionListener action, Color color) {
        MetrolineButton button = new MetrolineButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Sans Serif", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.addActionListener(action);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {button.setBackground(StyleUtil.changeColorShade(color, 20));}
            public void mouseExited(MouseEvent e) {button.setBackground(color);}
        });
        return button;
    }
    public static MetrolineButton createMetrolineInGameButton(String text, ActionListener action) {
        MetrolineButton button = new MetrolineButton(text);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Sans Serif", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.addActionListener(action);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {button.setBackground(new Color(79, 155, 155));}
            public void mouseEntered(MouseEvent e) { button.setBackground(new Color(80, 80, 80)); }
            public void mouseExited(MouseEvent e) { button.setBackground(new Color(60, 60, 60)); }
        });

        return button;
    }
    public void setLocalizedTooltip(String tooltipKey) {
        if (tooltipKey != null && !tooltipKey.isEmpty()) {
            this.setToolTipText(tooltipKey);

            // Добавляем слушатель для динамического обновления подсказки при изменении языка
            this.addPropertyChangeListener("ancestor", evt -> {
                if (this.getToolTipText() != null) {
                    this.setToolTipText(tooltipKey);
                }
            });
        }
    }
    private void initDefaultStyle() {
        setForeground(DEFAULT_FOREGROUND);
        setFont(DEFAULT_FONT);
        setBackground(DEFAULT_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HOVER_BACKGROUND);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(DEFAULT_BACKGROUND);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(CLICK_BACKGROUND);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(HOVER_BACKGROUND);
            }
        });
    }

    private void initCustomColorStyle(Color baseColor) {
        setForeground(DEFAULT_FOREGROUND);
        setFont(DEFAULT_FONT);
        setBackground(baseColor);
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(StyleUtil.changeColorShade(baseColor, 20));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(baseColor);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(StyleUtil.changeColorShade(baseColor, -20));
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(StyleUtil.changeColorShade(baseColor, 20));
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
                setBackground(HOVER_BACKGROUND);
            }
            @Override
            public void mouseExited(MouseEvent e) {
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
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Убираем стандартную отрисовку границы
    }

    @Override
    public void updateTranslation() {
        setText(LngUtil.translatable(translationKey));
    }
}
