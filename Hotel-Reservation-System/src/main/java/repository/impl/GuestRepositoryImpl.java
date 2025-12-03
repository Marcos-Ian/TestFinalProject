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
}
