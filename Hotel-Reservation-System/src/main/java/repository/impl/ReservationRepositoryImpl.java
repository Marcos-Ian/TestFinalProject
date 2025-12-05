package repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Guest;
import model.Reservation;
import model.ReservationStatus;
import model.RoomType;
import repository.ReservationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepositoryImpl implements ReservationRepository {
    private final EntityManager entityManager;

    public ReservationRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Reservation save(Reservation reservation) {
        return saveOrUpdate(reservation, reservation.getRooms());
    }

    @Override
    public Reservation saveOrUpdate(Reservation reservation, List<RoomType> rooms) {
        entityManager.getTransaction().begin();
        if (rooms != null) {
            reservation.setRooms(new ArrayList<>(rooms));
        }

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
    public List<Reservation> findByGuest(Guest guest) {
        if (guest == null || guest.getId() == null) {
            return List.of();
        }
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.guest.id = :guestId",
                Reservation.class);
        query.setParameter("guestId", guest.getId());
        return query.getResultList();
    }

    @Override
    public List<Reservation> searchReservations(String guestName, String phone, String email, LocalDate start, LocalDate end, ReservationStatus status) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Reservation r WHERE 1=1");

        if (guestName != null && !guestName.isBlank()) {
            jpql.append(" AND (LOWER(r.guest.firstName) LIKE LOWER(CONCAT('%', :guestName, '%'))")
                    .append(" OR LOWER(r.guest.lastName) LIKE LOWER(CONCAT('%', :guestName, '%')))");
        }
        if (phone != null && !phone.isBlank()) {
            jpql.append(" AND r.guest.phone LIKE CONCAT('%', :phone, '%')");
        }
        if (email != null && !email.isBlank()) {
            jpql.append(" AND LOWER(r.guest.email) LIKE LOWER(CONCAT('%', :email, '%'))");
        }
        if (start != null) {
            jpql.append(" AND r.checkIn >= :start");
        }
        if (end != null) {
            jpql.append(" AND r.checkOut <= :end");
        }
        if (status != null) {
            jpql.append(" AND r.status = :status");
        }

        TypedQuery<Reservation> query = entityManager.createQuery(jpql.toString(), Reservation.class);

        if (guestName != null && !guestName.isBlank()) {
            query.setParameter("guestName", guestName);
        }
        if (phone != null && !phone.isBlank()) {
            query.setParameter("phone", phone);
        }
        if (email != null && !email.isBlank()) {
            query.setParameter("email", email);
        }
        if (start != null) {
            query.setParameter("start", start);
        }
        if (end != null) {
            query.setParameter("end", end);
        }
        if (status != null) {
            query.setParameter("status", status);
        }

        return query.getResultList();
    }

    @Override
    public boolean hasConflict(RoomType room, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        String jpql = "SELECT COUNT(r) FROM Reservation r " +
                "JOIN r.rooms rr " +
                "WHERE rr = :room " +
                "AND r.status <> :cancelled " +
                "AND (:excludeId IS NULL OR r.id <> :excludeId) " +
                "AND r.checkIn < :checkOut " +
                "AND r.checkOut > :checkIn";

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("room", room);
        query.setParameter("cancelled", ReservationStatus.CANCELLED);
        query.setParameter("excludeId", excludeReservationId);
        query.setParameter("checkIn", checkIn);
        query.setParameter("checkOut", checkOut);

        Long count = query.getSingleResult();
        return count != null && count > 0;
    }
}
