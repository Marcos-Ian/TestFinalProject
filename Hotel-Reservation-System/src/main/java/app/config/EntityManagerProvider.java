package app.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Singleton holder for the JPA EntityManagerFactory and helper to produce EntityManagers.
 */
public final class EntityManagerProvider {
    private static EntityManagerFactory emf;

    private EntityManagerProvider() {
    }

    /**
     * Lazily initialize and return the shared EntityManagerFactory.
     */
    private static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            // Persistence unit name should match META-INF/persistence.xml when added.
            emf = Persistence.createEntityManagerFactory("hotelPU");
        }
        return emf;
    }

    /**
     * Obtain a new EntityManager instance from the shared factory.
     */
    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }
}
