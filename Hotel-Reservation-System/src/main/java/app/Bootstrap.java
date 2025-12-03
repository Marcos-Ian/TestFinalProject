package app;

import app.config.EntityManagerProvider;
import util.LoggingProvider;

/**
 * Application bootstrap responsible for wiring dependencies and starting the JavaFX UI.
 * In a full implementation this would initialize the JavaFX Application subclass and
 * inject services into controllers via FXMLLoader factories.
 */
public class Bootstrap {

    public static void main(String[] args) {
        LoggingProvider.configure();
        EntityManagerProvider.getInstance();
        // TODO: launch JavaFX Application once UI controllers are implemented
        System.out.println("Hotel Reservation System bootstrap initialized.");
    }
}
