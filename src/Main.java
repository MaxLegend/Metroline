import metroline.MainFrame;

import metroline.util.MetroLogger;


import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Main class my game Metroline.
 * @author  Tesmio
 * @Create Application birth July 17, 2025
 */
public class Main {
    public static void main(String[] args) {
        try {

            MetroLogger.setup();

            MetroLogger.logInfo( "=== Start Metroline ===");
            MetroLogger.logInfo("Java version: " + System.getProperty("java.version"));
            MetroLogger.logInfo( "OS: " + System.getProperty("os.name"));

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                MetroLogger.logError("Uncaught exception in thread: " + thread.getName(), throwable);
                showFatalError("Critical error in thread " + thread.getName(), throwable);
            });

            SwingUtilities.invokeLater(() -> {
                try {
                    ImageIcon icon = new ImageIcon(Main.class.getResource("/icon.png"));
                    MainFrame frame = new MainFrame();

                    frame.setIconImage(icon.getImage());

                    // === Настройка "всегда во весь экран в оконном режиме" ===
                    // Сначала пробуем стандартную максимизацию
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

                    // Резерв: если окно не максимизировалось (проверяем после установки состояния)
                    if (frame.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                        Rectangle bounds = ge.getMaximumWindowBounds(); // Рабочая область БЕЗ панели задач
                        frame.setBounds(bounds);
                    }

                    // Запрещаем изменение размера, если требуется строго "всегда во весь экран"
                    // frame.setResizable(false); // Раскомментируйте, если нужно запретить изменение размера

                    frame.setVisible(true);
                    MetroLogger.logInfo("Main window launched successfully (maximized window mode)");
                } catch (Exception e) {
                    MetroLogger.logError("Failed to create main window", e);
                    showFatalError("Failed to create main window", e);
                }
            });

        } catch (Throwable t) {
            MetroLogger.logError( "Application startup failed", t);
            showFatalError("Application startup failed", t);
        }
    }

    private static void showFatalError(String title, Throwable throwable) {
        Date d = new Date();
        ZonedDateTime now = ZonedDateTime.now();

        try (PrintWriter pw = new PrintWriter("metroline_crash_"
                + now.getDayOfMonth() + "_"
                + now.getMonth().getValue() + "_"
                + now.getYear() + "_in_"
                + now.getHour() + "_"
                + now.getMinute() + "_"
                + now.getSecond()
                + ".log")) {
            pw.println("=== CRASH REPORT ===");
            pw.println("Time: " + new Date());
            pw.println("Title: " + title);
            pw.println("\nStack Trace:");
            throwable.printStackTrace(pw);

            pw.println("\nSystem properties:");
            System.getProperties().forEach((k,v) -> pw.println(k + "=" + v));
        } catch (Exception e) {
            System.err.println("Failed to write crash report: " + e.getMessage());
        }

        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        JTextArea textArea = new JTextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setBackground(new Color(30, 30, 30));
        textArea.setForeground(Color.WHITE);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));

        JOptionPane.showMessageDialog(
                null,
                scrollPane,
                title,
                JOptionPane.ERROR_MESSAGE
        );

        MetroLogger.close();
        System.exit(1);
    }
}