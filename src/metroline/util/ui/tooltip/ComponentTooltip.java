package metroline.util.ui.tooltip;

import javax.swing.*;
import java.awt.*;

public class ComponentTooltip {
    private static JFrame parentFrame;
    private JLabel tooltipLabel;
    private Component currentTooltipComponent;
    private void init() {
        // Создаем label для тултипов в правом верхнем углу
        tooltipLabel = new JLabel("");
        tooltipLabel.setForeground(Color.WHITE);
        tooltipLabel.setBackground(new Color(60, 60, 60));
        tooltipLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));
        tooltipLabel.setOpaque(true);
        tooltipLabel.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        tooltipLabel.setVisible(false);

        // Размещаем в слоистой панели поверх всего
        JLayeredPane layeredPane = parentFrame.getLayeredPane();
        layeredPane.add(tooltipLabel, JLayeredPane.POPUP_LAYER);
    }
    private void updateTooltipPosition() {
        if (currentTooltipComponent != null && tooltipLabel.isVisible()) {
            Point componentLocation = currentTooltipComponent.getLocationOnScreen();
            Point frameLocation = parentFrame.getLocationOnScreen();

            int x = componentLocation.x - frameLocation.x;
            int y = componentLocation.y - frameLocation.y - tooltipLabel.getPreferredSize().height - 15;

            // Проверяем, чтобы тултип не выходил за границы фрейма
            if (y < 0) {
                y = componentLocation.y - frameLocation.y + currentTooltipComponent.getHeight() + 5;
            }

            tooltipLabel.setBounds(x, y,
                    tooltipLabel.getPreferredSize().width,
                    tooltipLabel.getPreferredSize().height);
        }
    }
    public void showTooltip(Component component, String text) {
        if (text == null || text.isEmpty()) {
            hideTooltip();
            return;
        }

        tooltipLabel.setText(text);
        tooltipLabel.setVisible(true);
        currentTooltipComponent = component;

        // Позиционируем тултип рядом с компонентом
        updateTooltipPosition();
    }

    public void hideTooltip() {
        tooltipLabel.setVisible(false);
        currentTooltipComponent = null;
    }
}
