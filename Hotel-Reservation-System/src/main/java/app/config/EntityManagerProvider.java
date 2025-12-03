package app.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Singleton holder for the JPA EntityManagerFactory.
 */
public final class EntityManagerProvider {
    private static EntityManagerProvider instance;
    private final EntityManagerFactory emf;

    private EntityManagerProvider() {
        // Persistence unit name should match META-INF/persistence.xml when added.
        this.emf = Persistence.createEntityManagerFactory("hotelPU");
    }

    public static synchronized EntityManagerProvider getInstance() {
        if (instance == null) {
            instance = new EntityManagerProvider();
        }
        return instance;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}
