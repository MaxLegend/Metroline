package metroline.util.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionListener;

public class StyleUtil {

    public static final Color BACKGROUND_COLOR = new Color(30, 30, 30);
    public static final Color BACKGROUND_COLOR2 = new Color(60, 60, 60);
    public static final Color FOREGROUND_COLOR = new Color(220, 220, 220);

    public static MetrolineLabel createMetrolineLabel(String text, int swing) {
        MetrolineLabel label = new MetrolineLabel(text, swing);
        label.setForeground(StyleUtil.FOREGROUND_COLOR);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        return label;
    }



    public static MetrolineCheckbox createMetrolineCheckBox(String text, String tooltipText) {
        MetrolineCheckbox checkBox = new MetrolineCheckbox(text,tooltipText);
        checkBox.setBackground(StyleUtil.BACKGROUND_COLOR);
        checkBox.setForeground(StyleUtil.FOREGROUND_COLOR);
        checkBox.setFont(new Font("Sans Serif", Font.BOLD, 13));
        checkBox.setFocusPainted(false);
        return checkBox;
    }
    public static Color changeColorShade(Color changedColor, int value) {
        return new Color(changedColor.getRed() + value, changedColor.getGreen() + value, changedColor.getBlue() + value);
    }


    public static Font getMetrolineFont(int size) {
        return new Font("Sans Serif", Font.BOLD, size);
    }
    public static JButton createSimpleMetrolineButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Sans Serif", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.addActionListener(action);
        return button;
    }








    public static class MetroScrollBarUI extends BasicScrollBarUI {
        private static final Color TRACK_COLOR = new Color(45, 45, 45);
        private static final Color THUMB_COLOR = new Color(100, 100, 100);
        private static final Color THUMB_HOVER_COLOR = new Color(120, 120, 120);

        @Override
        protected void configureScrollBarColors() {
            this.trackColor = TRACK_COLOR;
            this.thumbColor = THUMB_COLOR;
            this.thumbHighlightColor = THUMB_HOVER_COLOR;
            this.thumbDarkShadowColor = THUMB_HOVER_COLOR;
            this.thumbLightShadowColor = THUMB_HOVER_COLOR;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TRACK_COLOR);
            g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 6, 6);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            int w = thumbBounds.width;
            int h = thumbBounds.height;

            g2.setColor(THUMB_COLOR);
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, w - 2, h, 6, 6);

            if (isThumbRollover()) {
                g2.setColor(THUMB_HOVER_COLOR);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, w - 2, h, 6, 6);
            }
        }

        public static void styleScrollPane(JScrollPane scrollPane) {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setUI(new MetroScrollBarUI());
            verticalScrollBar.setBackground(TRACK_COLOR);
            verticalScrollBar.setUnitIncrement(16);
            verticalScrollBar.setBlockIncrement(64);
            verticalScrollBar.setPreferredSize(new Dimension(10, Integer.MAX_VALUE));

            JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
            if (horizontalScrollBar != null) {
                horizontalScrollBar.setUI(new MetroScrollBarUI());
                horizontalScrollBar.setBackground(TRACK_COLOR);
            }
        }
    }
}
