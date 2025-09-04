package metroline.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.logging.*;
/**
 * Custom logger for Metroline
 * @Autor Tesmio
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
                return String.format("[%1$tF %1$tT] [%2$s] %3$s%n",
                        new Date(record.getMillis()),
                        levelName,
                        record.getMessage()
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

    public static void logInfo(String message) {
        LOGGER.info(message);
    }

    public static void logWarning(String message) {
        LOGGER.warning(message);
    }

    public static void logError(String message, Throwable thrown) {
        LOGGER.log(Level.SEVERE, message, thrown);
    }
    public static void logError(String message) {
        LOGGER.log(Level.SEVERE, message);
    }
    public static void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
