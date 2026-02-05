package metroline.util.ui;

import javax.swing.*;
import java.awt.*;

public class UserInterfaceUtil {
    /**
     * Показывает временное сообщение (исчезает через 2 секунды)
     * @param message Текст сообщения
     * @param isError true для сообщения об ошибке (красный цвет)
     */
    public static void showTimedMessage(String message, boolean isError, int delay) {
        // Получаем активное окно для родительского компонента
        Frame parentFrame = null;
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible()) {
                parentFrame = frame;
                break;
            }
        }

        // Создаем диалоговое окно
        JDialog popup = new JDialog(parentFrame, "", true);
        popup.setUndecorated(true);
        popup.setBackground(new Color(0, 0, 0, 0)); // Прозрачный фон окна
        popup.setModal(false);

        // Создаем панель с полупрозрачным черным фоном
        JPanel messagePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // Рисуем полупрозрачный черный прямоугольник
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 180)); // Полупрозрачный черный
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        messagePanel.setOpaque(false); // Делаем панель прозрачной, чтобы был виден наш фон
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Настраиваем стиль текста
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Sans Serif", Font.PLAIN, 14));
        label.setForeground(isError ? new Color(255, 100, 100) : Color.WHITE); // Белый текст для контраста

        messagePanel.add(label, BorderLayout.CENTER);
        popup.getContentPane().add(messagePanel);
        popup.pack();

        // Позиционируем по центру экрана
        popup.setLocationRelativeTo(null);

        // Делаем окно полупрозрачным
        popup.setOpacity(0.9f);

        // Показываем сообщение
        popup.setVisible(true);

        // Таймер для автоматического закрытия
        Timer timer = new Timer(delay, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    public static void showTimedMessageDownScreen(String message, boolean isError, int delay) {
        // Получаем главное окно приложения
        Frame mainFrame = null;
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible() && frame.getTitle().contains("Main")) { // Ищем главное окно
                mainFrame = frame;
                break;
            }
        }

        // Если не нашли главное окно, используем первое видимое
        if (mainFrame == null) {
            for (Frame frame : Frame.getFrames()) {
                if (frame.isVisible()) {
                    mainFrame = frame;
                    break;
                }
            }
        }

        // Создаем диалоговое окно
        JDialog popup = new JDialog(mainFrame, "", true);
        popup.setUndecorated(true);
        popup.setBackground(new Color(0, 0, 0, 0)); // Прозрачный фон окна
        popup.setModal(false);

        // Создаем панель с полупрозрачным черным фоном
        JPanel messagePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // Рисуем полупрозрачный черный прямоугольник
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 180)); // Полупрозрачный черный
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        messagePanel.setOpaque(false); // Делаем панель прозрачной, чтобы был виден наш фон
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Настраиваем стиль текста
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setForeground(isError ? new Color(255, 100, 100) : Color.WHITE); // Белый текст для контраста

        messagePanel.add(label, BorderLayout.CENTER);
        popup.getContentPane().add(messagePanel);
        popup.pack();

        // Позиционируем внизу главного окна по центру
        if (mainFrame != null) {
            int bottomMargin = 120; // Отступ от нижнего края окна
            popup.setLocation(
                    mainFrame.getX() + (mainFrame.getWidth() - popup.getWidth()) / 2, // Центрируем по горизонтали
                    mainFrame.getY() + mainFrame.getHeight() - popup.getHeight() - bottomMargin // Размещаем внизу окна
            );
        } else {
            // Если главное окно не найдено, центрируем на экране
            popup.setLocationRelativeTo(null);
        }

        // Делаем окно полупрозрачным
        popup.setOpacity(0.9f);

        // Показываем сообщение
        popup.setVisible(true);

        // Таймер для автоматического закрытия
        Timer timer = new Timer(delay, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }
}
