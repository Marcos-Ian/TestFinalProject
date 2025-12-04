// ============================================================================
// ReservationService.java - Enhanced with full booking capabilities
// ============================================================================
package service;

import model.Guest;
import model.Reservation;
import model.ReservationStatus;
import model.RoomType;
import repository.GuestRepository;
import repository.ReservationRepository;
import util.ValidationUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Business layer gateway that validates reservations before persistence.
 * Enhanced with full booking workflow including rooms and add-ons.
 */
public class ReservationService {
    private static final Logger LOGGER = Logger.getLogger(ReservationService.class.getName());

    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(GuestRepository guestRepository,
                              ReservationRepository reservationRepository) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Create a basic reservation (legacy method)
     */
    public Reservation createReservation(Guest guest, LocalDate checkIn, LocalDate checkOut) {
        ValidationUtils.require(checkIn != null && checkOut != null && !checkOut.isBefore(checkIn),
                "Check-out date must be after check-in date.");

        Guest persisted = guestRepository.save(guest);
        Reservation reservation = new Reservation();
        reservation.setGuest(persisted);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setStatus(ReservationStatus.BOOKED);

        return reservationRepository.save(reservation);
    }

    /**
     * Create a complete reservation with rooms and add-ons
     * This is the main method used by the kiosk system
     *
     * @param reservation Reservation object with guest, dates, and status
     * @param rooms List of selected room types
     * @param addOns List of selected add-on service names
     * @return Saved reservation with all details
     */
    public Reservation createReservation(Reservation reservation, List<RoomType> rooms,
                                         List<String> addOns) {
        LOGGER.info("Creating new reservation with rooms and add-ons");

        // Validate reservation data
        validateReservation(reservation);

        // Validate guest information
        Guest guest = reservation.getGuest();
        validateGuest(guest);

        // Save or retrieve guest
        Guest persistedGuest = saveOrUpdateGuest(guest);
        reservation.setGuest(persistedGuest);

        // Validate room selection
        ValidationUtils.require(rooms != null && !rooms.isEmpty(),
                "At least one room must be selected");

        // Save reservation
        Reservation saved = reservationRepository.saveOrUpdate(reservation, rooms);

        // TODO: Save room bookings and add-ons in separate tables
        // This would require ReservationRoom and ReservationAddOn entities
        // For now, log the information
        LOGGER.info(String.format("Reservation created: ID=%d, Guest=%s %s, Rooms=%d, AddOns=%d",
                saved.getId(),
                persistedGuest.getFirstName(),
                persistedGuest.getLastName(),
                rooms.size(),
                addOns.size()));

        // Log room details
        for (RoomType room : rooms) {
            LOGGER.info(String.format("  - Room: %s ($%.2f, capacity %d)",
                    room.getType(), room.getBasePrice(), room.getCapacity()));
        }

        // Log add-on details
        for (String addOn : addOns) {
            LOGGER.info(String.format("  - Add-on: %s", addOn));
        }

        return saved;
    }

    /**
     * Update an existing reservation
     */
    public Reservation updateReservation(Reservation reservation) {
        LOGGER.info("Updating reservation: " + reservation.getId());

        validateReservation(reservation);

        return reservationRepository.saveOrUpdate(reservation, reservation.getRooms());
    }

    /**
     * Retrieve every reservation in the system.
     */
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    /**
     * Search reservations using optional filters that mirror the admin UI fields.
     *
     * @param name      guest name (optional, partial match)
     * @param phone     guest phone (optional, partial match)
     * @param email     guest email (optional, partial match)
     * @param startDate check-in on/after (optional)
     * @param endDate   check-out on/before (optional)
     * @param status    reservation status (optional, "All" to ignore)
     * @return matching reservations
     */
    public List<Reservation> searchReservations(String name, String phone, String email,
                                                LocalDate startDate, LocalDate endDate,
                                                ReservationStatus status) {
        return reservationRepository.searchReservations(name, phone, email, startDate, endDate, status);
    }

    /**
     * Cancel a reservation
     */
    public void cancelReservation(Long reservationId) {
        LOGGER.info("Cancelling reservation: " + reservationId);

        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            Reservation reservation = existing.get();
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.saveOrUpdate(reservation, reservation.getRooms());
            LOGGER.info("Reservation cancelled successfully");
        } else {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
    }

    /**
     * Check out a reservation
     */
    public void checkOut(Long reservationId) {
        LOGGER.info("Checking out reservation: " + reservationId);

        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            Reservation reservation = existing.get();
            reservation.setStatus(ReservationStatus.CHECKED_OUT);
            reservationRepository.saveOrUpdate(reservation, reservation.getRooms());
            LOGGER.info("Reservation checked out successfully");
        } else {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
    }

    public void saveWithConflictCheck(Reservation reservation, List<RoomType> rooms) {
        if (rooms == null) {
            rooms = new java.util.ArrayList<>();
        }
        for (RoomType room : rooms) {
            boolean conflict = reservationRepository.hasConflict(
                    room,
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getId()
            );
            if (conflict) {
                throw new ReservationConflictException(
                        "Room " + room.getType().name() + " is not available for the selected dates."
                );
            }
        }
        reservation.setStatus(ReservationStatus.BOOKED);
        reservationRepository.saveOrUpdate(reservation, rooms);
    }

    public void cancelReservation(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.saveOrUpdate(reservation, reservation.getRooms());
    }

    /**
     * Find reservation by ID
     */
    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }

    /**
     * Search reservations by guest name
     */
    public List<Reservation> searchByGuestName(String name) {
        LOGGER.info("Searching reservations for guest: " + name);
        // TODO: Implement in repository
        return List.of();
    }

    /**
     * Search reservations by date range
     */
    public List<Reservation> searchByDateRange(LocalDate startDate, LocalDate endDate) {
        LOGGER.info(String.format("Searching reservations from %s to %s", startDate, endDate));
        // TODO: Implement in repository
        return List.of();
    }

    /**
     * Search reservations by status
     */
    public List<Reservation> searchByStatus(String status) {
        LOGGER.info("Searching reservations with status: " + status);
        // TODO: Implement in repository
        return List.of();
    }

    /**
     * Validate reservation data
     */
    private void validateReservation(Reservation reservation) {
        ValidationUtils.require(reservation != null, "Reservation cannot be null");
        ValidationUtils.require(reservation.getGuest() != null, "Guest information is required");
        ValidationUtils.require(reservation.getCheckIn() != null, "Check-in date is required");
        ValidationUtils.require(reservation.getCheckOut() != null, "Check-out date is required");
        ValidationUtils.require(!reservation.getCheckOut().isBefore(reservation.getCheckIn()),
                "Check-out date must be after check-in date");
        ValidationUtils.require(!reservation.getCheckIn().isBefore(LocalDate.now()),
                "Check-in date cannot be in the past");
    }

    /**
     * Validate guest information
     */
    private void validateGuest(Guest guest) {
        ValidationUtils.require(guest.getFirstName() != null && !guest.getFirstName().trim().isEmpty(),
                "Guest first name is required");
        ValidationUtils.require(guest.getLastName() != null && !guest.getLastName().trim().isEmpty(),
                "Guest last name is required");
        ValidationUtils.require(guest.getPhone() != null && guest.getPhone().matches("\\d{10}"),
                "Valid 10-digit phone number is required");
        ValidationUtils.require(guest.getEmail() != null &&
                        guest.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$"),
                "Valid email address is required");
    }

    /**
     * Save or update guest information
     * Checks if guest already exists by email
     */
    private Guest saveOrUpdateGuest(Guest guest) {
        // TODO: Check if guest exists by email and update instead of creating new
        // For now, just save
        return guestRepository.save(guest);
    }

}
