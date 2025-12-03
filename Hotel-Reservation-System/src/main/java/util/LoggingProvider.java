package util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures java.util.logging with a rotating file handler according to the assignment.
 */
public final class LoggingProvider {
    private static boolean configured = false;
    private static final Logger ROOT = Logger.getLogger("HotelReservationSystem");

    private LoggingProvider() {}

    public static synchronized void configure() {
        if (configured) return;
        try {
            FileHandler handler = new FileHandler("logs/system_logs.%g.log", 1_000_000, 10, true);
            handler.setFormatter(new SimpleFormatter());
            ROOT.addHandler(handler);
            ROOT.setLevel(Level.INFO);
            configured = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger(Class<?> type) {
        configure();
        return Logger.getLogger(type.getName());
    }
}
