package metroline.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.logging.*;
/**
 * Custom logger for Metroline
 * @author Tesmio
 */
public class MetroLogger {
    private static final Logger LOGGER = Logger.getLogger("Metroline");
    private static FileHandler fileHandler;

    public static void setup() throws IOException {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        Formatter formatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                String levelName = formatLevel(record.getLevel());
                // Получаем источник вызова из параметра record.getSourceClassName() и т.д.
                String source = record.getSourceClassName() != null
                        ? String.format("[%s][%s]",
                        record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf('.') + 1),
                        record.getSourceMethodName())
                        : "";

                return String.format("[%1$tF %1$tT] [%2$s] %4$s %3$s%n",
                        new Date(record.getMillis()),
                        levelName,
                        record.getMessage(),
                        source.isEmpty() ? "" : source + " "
                );
            }

            private String formatLevel(Level level) {
                if (level == Level.SEVERE) return "ERROR";
                if (level == Level.WARNING) return "WARN";
                if (level == Level.INFO) return "INFO";
                if (level == Level.CONFIG) return "CONF";
                if (level == Level.FINE) return "FINE";
                if (level == Level.FINER) return "FINE2";
                if (level == Level.FINEST) return "FINE3";
                return level.getName();
            }
        };

        ConsoleHandler consoleHandler = new ConsoleHandler() {
            @Override
            protected void setOutputStream(OutputStream out) throws SecurityException {
                super.setOutputStream(System.out);
            }
        };
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.ALL);
        rootLogger.addHandler(consoleHandler);

        fileHandler = new FileHandler("metroline.log", false);
        fileHandler.setFormatter(formatter);
        fileHandler.setLevel(Level.ALL);
        rootLogger.addHandler(fileHandler);

        rootLogger.setLevel(Level.INFO);
    }

    // Вспомогательный метод для получения вызывающего класса/метода
    private static StackTraceElement getCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Пропускаем: [0] Thread.getStackTrace, [1] MetroLogger.getCaller, [2] MetroLogger.logXxx
        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            // Пропускаем внутренние классы логгера и системные вызовы
            if (!className.equals(MetroLogger.class.getName()) &&
                    !className.startsWith("java.") &&
                    !className.startsWith("javax.") &&
                    !className.startsWith("sun.") &&
                    !className.startsWith("jdk.")) {
                return element;
            }
        }
        return stackTrace[3]; // fallback
    }

    private static void logWithSource(Level level, String message, Throwable thrown) {
        StackTraceElement caller = getCaller();
        LogRecord record = new LogRecord(level, message);
        record.setSourceClassName(caller.getClassName());
        record.setSourceMethodName(caller.getMethodName());
        //   record.setSourceLineNumber(caller.getLineNumber());
        if (thrown != null) {
            record.setThrown(thrown);
        }
        LOGGER.log(record);
    }

    public static void logInfo(String message) {
        logWithSource(Level.INFO, message, null);
    }

    public static void logWarning(String message) {
        logWithSource(Level.WARNING, message, null);
    }

    public static void logError(String message, Throwable thrown) {
        logWithSource(Level.SEVERE, message, thrown);
    }

    public static void logError(String message) {
        logWithSource(Level.SEVERE, message, null);
    }

    public static void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
