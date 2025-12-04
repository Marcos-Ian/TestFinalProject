// ============================================================================
// ReservationRepository - Enhanced with search methods
// ============================================================================
package repository;

import model.Reservation;
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
}
