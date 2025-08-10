package metroline.util;

import javax.swing.*;
import java.awt.*;

public class MessageUtil {
    /**
     * Показывает временное сообщение (исчезает через 2 секунды)
     * @param message Текст сообщения
     * @param isError true для сообщения об ошибке (красный цвет)
     */
    public static void showTimedMessage(String message, boolean isError, int delay) {
        // Создаем прозрачную панель для сообщения
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Настраиваем стиль текста
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setForeground(isError ? new Color(255, 100, 100) : Color.BLACK);

        messagePanel.add(label, BorderLayout.CENTER);

        // Создаем прозрачное окно
        JWindow popup = new JWindow();
        popup.getContentPane().setBackground(new Color(0, 0, 0, 0));
        popup.getContentPane().add(messagePanel);
        popup.pack();

        // Позиционируем по центру экрана
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        popup.setLocation(
                (screenSize.width - popup.getWidth()) / 2,
                (screenSize.height - popup.getHeight()) / 2
        );

        // Делаем окно полупрозрачным
        popup.setOpacity(0.9f);

        // Показываем сообщение
        popup.setVisible(true);

        // Таймер для автоматического закрытия через 2 секунды
        Timer timer = new Timer(delay, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }
}
