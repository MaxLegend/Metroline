package metroline.util.ui;

import javax.swing.*;
import java.awt.*;

public class PrerenderIcon {
    public class StationIcon implements Icon {
        private final int width, height;
        private final Color color;

        public StationIcon(int width, int height, Color color) {
            this.width = width;
            this.height = height;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.fillOval(x, y, width, height);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(x, y, width, height);
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }
    }
    public class TunnelIcon implements Icon {
        private final int width, height;
        private final Color color;

        public TunnelIcon(int width, int height, Color color) {
            this.width = width;
            this.height = height;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.fillOval(x, y, width, height);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(x, y, width, height);
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }
    }
}
