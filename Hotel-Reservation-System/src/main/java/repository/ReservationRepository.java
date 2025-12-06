package repository;

import model.Guest;
import model.Reservation;
import model.ReservationStatus;
import model.RoomType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Reservation saveOrUpdate(Reservation reservation, List<RoomType> rooms);

    Optional<Reservation> findById(Long id);

    List<Reservation> findAll();

    void delete(Long id);

    int countBookedRooms(String roomType, LocalDate checkIn, LocalDate checkOut);

    List<Reservation> findByGuestName(String name);

    List<Reservation> findByDateRange(LocalDate from, LocalDate to);

    List<Reservation> findByStatus(String status);

    List<Reservation> findByGuestPhone(String phone);

    List<Reservation> findByGuest(Guest guest);

    boolean existsByGuest(Guest guest);

    boolean existsByGuestAndStatus(Guest guest, ReservationStatus status);

    /**
     * Advanced search used by the Admin reservation search screen.
     */
    List<Reservation> searchReservations(
            String guestName,
            String phone,
            String email,
            LocalDate start,
            LocalDate end,
            ReservationStatus status
    );

    /**
     * Used by FeedbackService: most recent reservation for a guest email,
     * ordered by check-out date (latest first).
     */
    Optional<Reservation> findMostRecentReservationByGuestEmail(String email);

    /**
     * Used when creating/updating reservations to check for overlapping bookings.
     */
    boolean hasConflict(RoomType room, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId);
}
