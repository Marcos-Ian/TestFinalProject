// ============================================================================
// RoomRepositoryImpl.java - JPA implementation of RoomRepository
// ============================================================================
package repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.RoomType;
import repository.RoomRepository;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JPA implementation of RoomRepository
 */
public class RoomRepositoryImpl implements RoomRepository {
    private static final Logger LOGGER = Logger.getLogger(RoomRepositoryImpl.class.getName());

    private final EntityManager entityManager;

    public RoomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public RoomType save(RoomType roomType) {
        try {
            entityManager.getTransaction().begin();

            if (roomType.getId() == null) {
                entityManager.persist(roomType);
                LOGGER.info("New room type created: " + roomType.getType());
            } else {
                roomType = entityManager.merge(roomType);
                LOGGER.info("Room type updated: " + roomType.getType());
            }

            entityManager.getTransaction().commit();
            return roomType;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            LOGGER.severe("Failed to save room type: " + e.getMessage());
            throw new RuntimeException("Failed to save room type", e);
        }
    }

    @Override
    public Optional<RoomType> findById(Long id) {
        try {
            RoomType roomType = entityManager.find(RoomType.class, id);
            return Optional.ofNullable(roomType);
        } catch (Exception e) {
            LOGGER.severe("Error finding room type by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<RoomType> findByType(RoomType.Type type) {
        try {
            TypedQuery<RoomType> query = entityManager.createQuery(
                    "SELECT r FROM RoomType r WHERE r.type = :type", RoomType.class);
            query.setParameter("type", type);

            RoomType roomType = query.getSingleResult();
            return Optional.of(roomType);
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.severe("Error finding room type by type: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<RoomType> findAll() {
        try {
            TypedQuery<RoomType> query = entityManager.createQuery(
                    "SELECT r FROM RoomType r ORDER BY r.basePrice", RoomType.class);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Error finding all room types: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public void delete(Long id) {
        try {
            entityManager.getTransaction().begin();

            RoomType roomType = entityManager.find(RoomType.class, id);
            if (roomType != null) {
                entityManager.remove(roomType);
                LOGGER.info("Room type deleted: " + id);
            }

            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            LOGGER.severe("Failed to delete room type: " + e.getMessage());
            throw new RuntimeException("Failed to delete room type", e);
        }
    }

    @Override
    public int countByType(RoomType.Type type) {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT COUNT(r) FROM RoomType r WHERE r.type = :type", Long.class);
            query.setParameter("type", type);

            return query.getSingleResult().intValue();
        } catch (Exception e) {
            LOGGER.severe("Error counting rooms by type: " + e.getMessage());
            return 0;
        }
    }
}