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
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r",
                Reservation.class);
        return query.getResultList();
    }

    @Override
    public void delete(Long id) {
        entityManager.getTransaction().begin();
        Reservation reservation = entityManager.find(Reservation.class, id);
        if (reservation != null) {
            entityManager.remove(reservation);
        }
        entityManager.getTransaction().commit();
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
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.status = :status",
                Reservation.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    @Override
    public List<Reservation> findByGuestPhone(String phone) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.guest.phone = :phone",
                Reservation.class);
        query.setParameter("phone", phone);
        return query.getResultList();
    }

    @Override
    public List<Reservation> searchReservations(String guestName, String phone, LocalDate start, LocalDate end, String status) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Reservation r WHERE 1=1");

        if (guestName != null && !guestName.isBlank()) {
            jpql.append(" AND (LOWER(r.guest.firstName) LIKE LOWER(CONCAT('%', :guestName, '%'))")
                    .append(" OR LOWER(r.guest.lastName) LIKE LOWER(CONCAT('%', :guestName, '%')))");
        }
        if (phone != null && !phone.isBlank()) {
            jpql.append(" AND r.guest.phone LIKE CONCAT('%', :phone, '%')");
        }
        if (start != null) {
            jpql.append(" AND r.checkIn >= :start");
        }
        if (end != null) {
            jpql.append(" AND r.checkOut <= :end");
        }
        if (status != null && !status.equalsIgnoreCase("All")) {
            jpql.append(" AND r.status = :status");
        }

        TypedQuery<Reservation> query = entityManager.createQuery(jpql.toString(), Reservation.class);

        if (guestName != null && !guestName.isBlank()) {
            query.setParameter("guestName", guestName);
        }
        if (phone != null && !phone.isBlank()) {
            query.setParameter("phone", phone);
        }
        if (start != null) {
            query.setParameter("start", start);
        }
        if (end != null) {
            query.setParameter("end", end);
        }
        if (status != null && !status.equalsIgnoreCase("All")) {
            query.setParameter("status", status);
        }

        return query.getResultList();
    }
}
