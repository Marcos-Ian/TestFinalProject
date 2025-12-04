// ============================================================================
// LoggingProvider.java - Centralized logging configuration
// ============================================================================
package util;

import java.io.IOException;
import java.util.logging.*;

/**
 * Centralized logging configuration for the application.
 * Configures rotating file handlers as per project requirements.
 */
public class LoggingProvider {
    private static final String LOG_FILE_PATTERN = "logs/system_logs.%g.log";
    private static final int LOG_FILE_SIZE = 1024 * 1024; // 1MB
    private static final int LOG_FILE_COUNT = 10;

    private static boolean configured = false;

    /**
     * Configure application-wide logging
     */
    public static synchronized void configure() {
        if (configured) {
            return;
        }

        try {
            // Get root logger
            Logger rootLogger = Logger.getLogger("");

            // Remove default console handler if exists
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof ConsoleHandler) {
                    rootLogger.removeHandler(handler);
                }
            }

            // Create logs directory if it doesn't exist
            java.io.File logDir = new java.io.File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // Create rotating file handler
            FileHandler fileHandler = new FileHandler(
                    LOG_FILE_PATTERN,
                    LOG_FILE_SIZE,
                    LOG_FILE_COUNT,
                    true
            );

            // Set formatter
            fileHandler.setFormatter(new SimpleFormatter() {
                private static final String FORMAT = "[%1$tF %1$tT] [%2$s] [%3$s] %4$s %n";

                @Override
                public synchronized String format(LogRecord record) {
                    return String.format(FORMAT,
                            new java.util.Date(record.getMillis()),
                            record.getLevel().getName(),
                            record.getLoggerName(),
                            record.getMessage()
                    );
                }
            });

            // Add file handler to root logger
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.INFO);

            // Also add console handler for development
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(consoleHandler);

            configured = true;

            Logger logger = Logger.getLogger(LoggingProvider.class.getName());
            logger.info("Logging system configured successfully");
            logger.info("Log files: " + LOG_FILE_PATTERN);
            logger.info("Max file size: " + (LOG_FILE_SIZE / 1024) + "KB");
            logger.info("Max file count: " + LOG_FILE_COUNT);

        } catch (IOException e) {
            System.err.println("Failed to configure logging: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get a logger for a specific class
     */
    public static Logger getLogger(Class<?> clazz) {
        if (!configured) {
            configure();
        }
        return Logger.getLogger(clazz.getName());
    }
}