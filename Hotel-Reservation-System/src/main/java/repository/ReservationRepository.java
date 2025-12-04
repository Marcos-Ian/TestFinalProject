// ============================================================================
// ReservationRepository - Enhanced with search methods
// ============================================================================
package repository;

import model.Reservation;
import model.ReservationStatus;
import model.RoomType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Reservation persistence operations.
 */
public interface ReservationRepository {

    /**
     * Save or update a reservation
     */
    Reservation save(Reservation reservation);

    /**
     * Save or update a reservation and its room associations.
     */
    Reservation saveOrUpdate(Reservation reservation, List<RoomType> rooms);

    /**
     * Find reservation by ID
     */
    Optional<Reservation> findById(Long id);

    /**
     * Find all reservations
     */
    List<Reservation> findAll();

    /**
     * Delete a reservation
     */
    void delete(Long id);

    /**
     * Count booked rooms for a specific room type in a date range
     * Used for availability checking
     */
    int countBookedRooms(String roomType, LocalDate checkIn, LocalDate checkOut);

    /**
     * Check for overlapping reservations for a room type.
     */
    boolean hasConflict(RoomType room, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId);

    /**
     * Find reservations by guest name (first or last)
     */
    List<Reservation> findByGuestName(String name);

    /**
     * Find reservations by date range
     */
    List<Reservation> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Find reservations by status
     */
    List<Reservation> findByStatus(String status);

    /**
     * Find reservations by guest phone
     */
    List<Reservation> findByGuestPhone(String phone);

    /**
     * Search reservations with optional filters.
     */
    List<Reservation> searchReservations(String guestName, String phone, String email, LocalDate start, LocalDate end, ReservationStatus status);
}
