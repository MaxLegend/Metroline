import screens.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        try {
            PrintStream logFile = new PrintStream(new File("metroline_startup.log"));
            System.setOut(logFile);
            System.setErr(logFile);
            // Устанавливаем обработчик неперехваченных исключений
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                showFatalError("Strange error in flow " + thread.getName(), throwable);
            });
            System.out.println("=== Start Metroline " + new Date() + " ===");
            System.out.println("Java version: " + System.getProperty("java.version"));
            System.out.println("OS: " + System.getProperty("os.name"));
            System.out.println("Launch...");
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            System.out.println("Main window is launched");
        }   catch (Throwable t) {
        try (PrintWriter pw = new PrintWriter("metroline_crash.log")) {
            t.printStackTrace(pw);
            pw.println("\nSystem properties:");
            System.getProperties().forEach((k,v) -> pw.println(k + "=" + v));
        } catch (Exception e) {
            // Если даже запись в файл не сработала
            JOptionPane.showMessageDialog(null,
                    "Critical error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        JOptionPane.showMessageDialog(null,
                "Application failed to start. See metroline_crash.log",
                "Error", JOptionPane.ERROR_MESSAGE);
    }
    }
    private static void showFatalError(String title, Throwable throwable) {
        System.err.println(title + ":");
        throwable.printStackTrace();

        // Формируем детализированное сообщение об ошибке
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String errorDetails = sw.toString();

        // Создаем панель с прокруткой для отображения полного стека ошибок
        JTextArea textArea = new JTextArea(errorDetails);
        textArea.setEditable(false);
        textArea.setBackground(new Color(30, 30, 30));
        textArea.setForeground(Color.WHITE);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));

        // Создаем кастомное диалоговое окно
        JOptionPane.showMessageDialog(
                null,
                scrollPane,
                title,
                JOptionPane.ERROR_MESSAGE
        );

        System.exit(1);
    }
}