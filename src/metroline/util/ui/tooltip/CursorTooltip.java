package metroline.util.ui.tooltip;

import javax.swing.*;
import java.awt.*;

public class CursorTooltip {
    private static JLabel tooltipLabel;
    private static JFrame parentFrame;
    private static String currentText = "";
    private static boolean isVisible = false;
    private static Timer followTimer;

    public static void init(JFrame frame) {
        parentFrame = frame;

        // Создаем label для тултипов
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
        layeredPane.add(tooltipLabel, JLayeredPane.DRAG_LAYER);

        // Создаем таймер для постоянного отслеживания курсора
        followTimer = new Timer(50, e -> {
            if (isVisible) {
                Point mousePos = MouseInfo.getPointerInfo().getLocation();
                Point framePos = parentFrame.getLocationOnScreen();
                updateTooltipPosition(mousePos.x - framePos.x, mousePos.y - framePos.y);
            }
        });
        followTimer.setRepeats(true);
    }

    public static void showTooltip(String text, int mouseX, int mouseY) {
        if (text == null || text.isEmpty()) {
            hideTooltip();
            return;
        }

        currentText = text;
        tooltipLabel.setText(text);
        tooltipLabel.setVisible(true);
        isVisible = true;

        // Запускаем таймер отслеживания
        followTimer.start();
        updateTooltipPosition(mouseX, mouseY);
    }

    public static void hideTooltip() {
        tooltipLabel.setVisible(false);
        isVisible = false;
        currentText = "";

        // Останавливаем таймер отслеживания
        followTimer.stop();
    }

    private static void updateTooltipPosition(int mouseX, int mouseY) {
        if (!isVisible) return;

        Dimension tooltipSize = tooltipLabel.getPreferredSize();
        Dimension frameSize = parentFrame.getSize();

        // Позиции относительно курсора с приоритетом по часовой стрелке
        Point[] possiblePositions = {
                new Point(mouseX + 15, mouseY + 15),  // правый низ (приоритет 1)
                new Point(mouseX - tooltipSize.width - 15, mouseY + 15),  // левый низ (приоритет 2)
                new Point(mouseX + 15, mouseY - tooltipSize.height - 15), // правый верх (приоритет 3)
                new Point(mouseX - tooltipSize.width - 15, mouseY - tooltipSize.height - 15) // левый верх (приоритет 4)
        };

        // Ищем первую подходящую позицию
        Point finalPosition = null;
        for (Point pos : possiblePositions) {
            if (pos.x >= 0 && pos.x + tooltipSize.width <= frameSize.width &&
                    pos.y >= 0 && pos.y + tooltipSize.height <= frameSize.height) {
                finalPosition = pos;
                break;
            }
        }

        // Если ни одна позиция не подходит, корректируем правый низ
        if (finalPosition == null) {
            finalPosition = new Point(
                    Math.max(0, Math.min(frameSize.width - tooltipSize.width, mouseX + 15)),
                    Math.max(0, Math.min(frameSize.height - tooltipSize.height, mouseY + 15))
            );
        }

        tooltipLabel.setBounds(finalPosition.x, finalPosition.y,
                tooltipSize.width, tooltipSize.height);
    }

    public static boolean isVisible() {
        return isVisible;
    }

    public static String getCurrentText() {
        return currentText;
    }
}