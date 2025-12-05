package repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Guest;
import repository.GuestRepository;

import java.util.List;
import java.util.Optional;

public class GuestRepositoryImpl implements GuestRepository {
    private final EntityManager entityManager;

    public GuestRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Guest save(Guest guest) {
        entityManager.getTransaction().begin();
        if (guest.getId() == null) {
            entityManager.persist(guest);
        } else {
            guest = entityManager.merge(guest);
        }
        entityManager.getTransaction().commit();
        return guest;
    }

    @Override
    public Optional<Guest> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Guest.class, id));
    }

    @Override
    public List<Guest> findByName(String name) {
        String pattern = "%" + name.toLowerCase() + "%";
        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE LOWER(g.firstName) LIKE :name OR LOWER(g.lastName) LIKE :name",
                Guest.class);
        query.setParameter("name", pattern);
        return query.getResultList();
    }

    @Override
    public List<Guest> searchGuests(String name, String phone, String email, String address) {
        String jpql = "SELECT g FROM Guest g " +
                "WHERE (:name IS NULL OR LOWER(g.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
                "OR LOWER(g.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
                "AND (:phone IS NULL OR g.phone LIKE CONCAT('%', :phone, '%')) " +
                "AND (:email IS NULL OR LOWER(g.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
                "AND (:address IS NULL OR LOWER(g.address) LIKE LOWER(CONCAT('%', :address, '%'))) " +
                "ORDER BY g.lastName, g.firstName";

        TypedQuery<Guest> query = entityManager.createQuery(jpql, Guest.class);
        query.setParameter("name", normalizeParam(name));
        query.setParameter("phone", normalizeParam(phone));
        query.setParameter("email", normalizeParam(email));
        query.setParameter("address", normalizeParam(address));
        return query.getResultList();
    }

    private String normalizeParam(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
