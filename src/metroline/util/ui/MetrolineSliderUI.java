package metroline.util.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;

import static javax.swing.SwingConstants.HORIZONTAL;

public  class MetrolineSliderUI extends ComponentUI {
    private static final int TRACK_HEIGHT = 4;
    private static final int THUMB_WIDTH = 12;
    private static final int THUMB_HEIGHT = 16;
    private static final Color TRACK_COLOR = new Color(60, 60, 60);
    private static final Color FILLED_TRACK_COLOR = new Color(60, 60, 60);
    private static final Color THUMB_COLOR = new Color(120, 120, 120);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 11);

    private MetrolineSlider slider;
    private Rectangle thumbRect = new Rectangle();
    private boolean isDragging = false;

    public static ComponentUI createUI(JComponent c) {
        return new MetrolineSliderUI((MetrolineSlider) c);
    }

    public MetrolineSliderUI(MetrolineSlider slider) {
        this.slider = slider;
        thumbRect.width = THUMB_WIDTH;
        thumbRect.height = THUMB_HEIGHT;

        // Добавляем слушатели мыши
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!slider.isEnabled()) return;
                slider.setValueIsAdjusting(true);
                updateSliderValue(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!slider.isEnabled()) return;
                slider.setValueIsAdjusting(false);
            }
        });

        slider.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!slider.isEnabled()) return;
                updateSliderValue(e);
            }
        });
    }

    private void updateSliderValue(MouseEvent e) {
        Rectangle trackBounds = getTrackBounds();
        float value;

        if (slider.getOrientation() == HORIZONTAL) {
            // Рассчитываем значение на основе положения мыши
            float pos = e.getX() - trackBounds.x;
            float extent = trackBounds.width;

            if (pos < 0) {
                value = slider.getMinValue();
            } else if (pos > extent) {
                value = slider.getMaxValue();
            } else {
                float range = slider.getMaxValue() - slider.getMinValue();
                value = slider.getMinValue() + (pos / extent) * range;
            }
        } else {
            // Вертикальная ориентация
            float pos = e.getY() - trackBounds.y;
            float extent = trackBounds.height;

            if (pos < 0) {
                value = slider.getMaxValue();
            } else if (pos > extent) {
                value = slider.getMinValue();
            } else {
                float range = slider.getMaxValue() - slider.getMinValue();
                value = slider.getMaxValue() - (pos / extent) * range;
            }
        }

        // Устанавливаем новое значение с учетом шага
        slider.setValue(value);
    }


    @Override
    public void installUI(JComponent c) {
        configureSlider((MetrolineSlider) c);
    }

    private void configureSlider(MetrolineSlider slider) {
        slider.setOpaque(false);
        slider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        paintTrack(g2d);
        paintFilledTrack(g2d);
        paintThumb(g2d);

        g2d.dispose();
    }

    private void paintTrack(Graphics2D g2d) {
        Rectangle trackBounds = getTrackBounds();
        int trackTop = trackBounds.y + (trackBounds.height - TRACK_HEIGHT) / 2;

        g2d.setColor(TRACK_COLOR);
        g2d.fillRect(trackBounds.x, trackTop, trackBounds.width, TRACK_HEIGHT);
    }

    private void paintFilledTrack(Graphics2D g2d) {
        Rectangle trackBounds = getTrackBounds();
        int trackTop = trackBounds.y + (trackBounds.height - TRACK_HEIGHT) / 2;

        float fillRatio = (slider.getValue() - slider.getMinValue()) /
                (slider.getMaxValue() - slider.getMinValue());
        int fillWidth = (int)(trackBounds.width * fillRatio);

        g2d.setColor(FILLED_TRACK_COLOR);
        g2d.fillRect(trackBounds.x, trackTop, fillWidth, TRACK_HEIGHT);
    }

    private void paintThumb(Graphics2D g2d) {
        updateThumbLocation();

        // Рисуем прямоугольный ползунок с закругленными углами
        RoundRectangle2D thumbShape = new RoundRectangle2D.Float(
                thumbRect.x, thumbRect.y,
                thumbRect.width, thumbRect.height,
                2, 2);

        g2d.setColor(THUMB_COLOR);
        g2d.fill(thumbShape);

        // Обводка для лучшей видимости
        g2d.setColor(THUMB_COLOR.darker());
        g2d.draw(thumbShape);
    }
    private void paintValue(Graphics2D g2d) {
        String valueText = String.format("%.1f", slider.getValue());
        FontMetrics fm = g2d.getFontMetrics(VALUE_FONT);
        int textWidth = fm.stringWidth(valueText);

        g2d.setFont(VALUE_FONT);
        g2d.setColor(StyleUtil.FOREGROUND_COLOR);

        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            int x = thumbRect.x + (thumbRect.width - textWidth) / 2;
            int y = thumbRect.y - 5;
            g2d.drawString(valueText, x, y);
        } else {
            int x = thumbRect.x + thumbRect.width + 5;
            int y = thumbRect.y + thumbRect.height / 2 + fm.getAscent() / 2;
            g2d.drawString(valueText, x, y);
        }
    }
    private Rectangle getTrackBounds() {
        Rectangle bounds = slider.getBounds();
        Insets insets = slider.getInsets();

        if (slider.getOrientation() == HORIZONTAL) {
            return new Rectangle(
                    insets.left + THUMB_WIDTH / 2,
                    insets.top + (bounds.height - insets.top - insets.bottom - TRACK_HEIGHT) / 2,
                    bounds.width - insets.left - insets.right - THUMB_WIDTH,
                    TRACK_HEIGHT
            );
        } else {
            return new Rectangle(
                    (bounds.width - TRACK_HEIGHT) / 2,
                    insets.top + THUMB_HEIGHT / 2,
                    TRACK_HEIGHT,
                    bounds.height - insets.top - insets.bottom - THUMB_HEIGHT
            );
        }
    }

    private void updateThumbLocation() {
        Rectangle trackBounds = getTrackBounds();
        float ratio = (slider.getValue() - slider.getMinValue()) /
                (slider.getMaxValue() - slider.getMinValue());

        if (slider.getOrientation() == HORIZONTAL) {
            thumbRect.x = trackBounds.x + (int)(ratio * trackBounds.width) - THUMB_WIDTH / 2;
            thumbRect.y = (slider.getHeight() - THUMB_HEIGHT) / 2;
        } else {
            thumbRect.x = (slider.getWidth() - THUMB_WIDTH) / 2;
            thumbRect.y = trackBounds.y + (int)((1 - ratio) * trackBounds.height) - THUMB_HEIGHT / 2;
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(200, 30);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(50, 20);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, 30);
    }
}
