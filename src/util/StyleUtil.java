package util;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;

public class StyleUtil {

    public static final Color BACKGROUND_COLOR = new Color(30, 30, 30);
    public static final Color FOREGROUND_COLOR = new Color(220, 220, 220);

    public static JLabel createMetrolineLabel(String text, int swing) {
        JLabel label = new JLabel(text, swing);
        label.setForeground(StyleUtil.FOREGROUND_COLOR);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        return label;
    }

    public static JSlider createMetrolineSlider(int min, int max, int value, String label, JLabel valueLabel) {
        JSlider slider = new JSlider(min, max, value) {
            @Override
            public void updateUI() {
                setUI(new StyleUtil.MetrolineSlider(this));
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0, 0, 0, 0)); // Прозрачный цвет
                    g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
                    g2.dispose();
                }
        }
        };

        slider.setBackground(StyleUtil.BACKGROUND_COLOR);
        slider.setForeground(StyleUtil.FOREGROUND_COLOR);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.setFont(new Font("Arial", Font.PLAIN, 20));
        slider.setFocusable(false);
        slider.setSnapToTicks(true);
        slider.addChangeListener(e -> {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                int value2 = source.getValue();
                int step = 10;
                int adjustedValue = (value2 / step) * step;
                if (adjustedValue != value2) {
                    source.setValue(adjustedValue);
                }
            }
        });
        slider.setModel(new DefaultBoundedRangeModel(value, 0, min, max) {
            @Override
            public void setValue(int n) {
                super.setValue((n / 10) * 10); // Округляем до ближайшего шага 10
            }
        });
        slider.setUI(new StyleUtil.MetrolineSlider(slider));

        valueLabel.setText(slider.getValue() + label);
        slider.addChangeListener(e -> {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                valueLabel.setText( source.getValue() + label);
            }
        });

        return slider;
    }

    public static JCheckBox createMetrolineCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setBackground(StyleUtil.BACKGROUND_COLOR);
        checkBox.setForeground(StyleUtil.FOREGROUND_COLOR);
        checkBox.setFont(new Font("Arial", Font.PLAIN, 14));
        checkBox.setFocusPainted(false);
        checkBox.setIcon(new StyleUtil.MetrolineCheckbox());
        checkBox.setSelectedIcon(new StyleUtil.MetrolineCheckbox(true));
        return checkBox;
    }
    public static Color changeColorShade(Color changedColor, int value) {
        return new Color(changedColor.getRed() + value, changedColor.getGreen() + value, changedColor.getBlue() + value);
    }

    public static JButton createMetrolineInGameButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
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
    public static JButton createSimpleMetrolineButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.addActionListener(action);
        return button;
    }
    public static JButton createMetrolineButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.setBackground(new Color(50, 50, 50));
        button.setForeground(Color.WHITE);
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
    public static JButton createMetrolineColorableButton(String text, ActionListener action, Color color) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
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
    public static class MetrolineSlider extends BasicSliderUI {
        private static final int TRACK_HEIGHT = 4;
        private static final int THUMB_WIDTH = 12;
        private static final int THUMB_HEIGHT = 16;

        public MetrolineSlider(JSlider slider) {
            super(slider);
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(THUMB_WIDTH, THUMB_HEIGHT);
        }
        protected void scrollDueToClickInTrack(int direction) {

            int value = slider.getValue();

            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                value = this.valueForXPosition(slider.getMousePosition().x);
            } else if (slider.getOrientation() == JSlider.VERTICAL) {
                value = this.valueForYPosition(slider.getMousePosition().y);
            }
            slider.setValue(value);
        }
        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle trackBounds = trackRect;
            int trackTop = trackBounds.y + (trackBounds.height - TRACK_HEIGHT) / 2;

            // Фон трека (полоска)
            g2d.setColor(new Color(60, 60, 60)); // Темный серый
            g2d.fillRect(trackBounds.x, trackTop, trackBounds.width, TRACK_HEIGHT);

            // Заполненная часть трека
            int fillWidth = thumbRect.x + (thumbRect.width / 2) - trackBounds.x;
            g2d.setColor(new Color(90, 90, 90)); // Серый
            g2d.fillRect(trackBounds.x, trackTop, fillWidth, TRACK_HEIGHT);
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Прямоугольный ползунок
            g2d.setColor(new Color(120, 120, 120)); // Светло-серый
            g2d.fillRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
        }

        @Override
        public void paintTicks(Graphics g) {
            // Убираем деления для минимализма
        }

        @Override
        public void paintLabels(Graphics g) {
            super.paintLabels(g);
        }

        @Override
        public void paintFocus(Graphics g) {}


    }

    public static class MetrolineCheckbox implements Icon {
        private static final int SIZE = 16;
        private static final int INNER_PADDING = 2;
        private final boolean selected;

        public MetrolineCheckbox() {
            this(false);
        }

        public MetrolineCheckbox(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Внешний квадрат (фон)
            g2.setColor(new Color(70, 70, 70)); // Темный серый
            g2.fillRect(x, y, SIZE, SIZE);

            // Граница
            g2.setColor(new Color(100, 100, 100)); // Серый
            g2.drawRect(x, y, SIZE, SIZE);

            if (selected) {
                // Внутренний квадрат (выбранное состояние)
                g2.setColor(new Color(150, 150, 150)); // Светло-серый
                g2.fillRect(x + INNER_PADDING, y + INNER_PADDING,
                        SIZE - 2*INNER_PADDING, SIZE - 2*INNER_PADDING);
            }
            g2.dispose();
        }



        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
