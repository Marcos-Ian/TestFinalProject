package repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Reservation;
import repository.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationRepositoryImpl implements ReservationRepository {
    private final EntityManager entityManager;

    public ReservationRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Reservation save(Reservation reservation) {
        entityManager.getTransaction().begin();
        if (reservation.getId() == null) {
            entityManager.persist(reservation);
        } else {
            reservation = entityManager.merge(reservation);
        }
        entityManager.getTransaction().commit();
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Reservation.class, id));
    }

    @Override
    public List<Reservation> findAll() {
        return List.of();
    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public int countBookedRooms(String roomType, LocalDate checkIn, LocalDate checkOut) {
        return 0;
    }

    @Override
    public List<Reservation> findByGuestName(String name) {
        String pattern = "%" + name.toLowerCase() + "%";
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE LOWER(r.guest.firstName) LIKE :name OR LOWER(r.guest.lastName) LIKE :name",
                Reservation.class);
        query.setParameter("name", pattern);
        return query.getResultList();
    }

    @Override
    public List<Reservation> findByDateRange(LocalDate from, LocalDate to) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.checkIn >= :from AND r.checkOut <= :to",
                Reservation.class);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    @Override
    public List<Reservation> findByStatus(String status) {
        return List.of();
    }

    @Override
    public List<Reservation> findByGuestPhone(String phone) {
        return List.of();
    }
}
