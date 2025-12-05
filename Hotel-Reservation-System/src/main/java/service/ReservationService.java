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
import repository.RoomRepository;
import security.AdminUser;
import security.AuthenticationService;
import util.ValidationUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import util.ActivityLogger;
// ADD these imports at the top of ReservationService.java

import config.PricingConfig;
import model.ReservationAddOn;
import service.BillingContext;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;
/**
 * Business layer gateway that validates reservations before persistence.
 * Enhanced with full booking workflow including rooms and add-ons.
 */
public class ReservationService {
    private static final Logger LOGGER = Logger.getLogger(ReservationService.class.getName());

    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final AuthenticationService authenticationService;

    public ReservationService(GuestRepository guestRepository,
                              ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              AuthenticationService authenticationService) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.authenticationService = authenticationService;
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

        List<RoomType> managedRooms = attachManagedRooms(rooms);

        // Calculate and save total amount using BillingContext
        long nights = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        double roomTotal = calculateRoomCharges(managedRooms, reservation.getCheckIn(), nights);
        double addOnTotal = calculateAddOnCharges(addOns, nights);
        double subtotal = roomTotal + addOnTotal;

        // Apply discount if exists
        double discountPercent = reservation.getDiscountPercent();
        double discountAmount = subtotal * (discountPercent / 100.0);
        double afterDiscount = subtotal - discountAmount;

        // Calculate tax
        double taxRate = 0.13; // 13% tax
        double tax = afterDiscount * taxRate;

        // Set total on reservation
        double totalAmount = afterDiscount + tax;
        reservation.setTotalAmount(totalAmount);

        // IMPORTANT: Clear and add to existing collection, don't replace it
        reservation.getAddOns().clear();

        // Create add-on entities and add them to the existing collection
        if (addOns != null && !addOns.isEmpty()) {
            Map<String, Double> addOnPrices = BillingContext.getAvailableAddOns();

            for (String addOnName : addOns) {
                Double price = addOnPrices.get(addOnName);
                if (price != null) {
                    boolean perNight = BillingContext.isAddOnPerNight(addOnName);
                    ReservationAddOn addOn = new ReservationAddOn(reservation, addOnName, price, perNight);
                    reservation.getAddOns().add(addOn);
                }
            }
        }

        // Save reservation with all associations
        Reservation saved = reservationRepository.saveOrUpdate(reservation, managedRooms);

        LOGGER.info(String.format("Reservation created: ID=%d, Guest=%s %s, Total=$%.2f, Rooms=%d, AddOns=%d",
                saved.getId(),
                persistedGuest.getFirstName(),
                persistedGuest.getLastName(),
                totalAmount,
                rooms.size(),
                saved.getAddOns().size()));

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
    private double calculateRoomCharges(List<RoomType> rooms, LocalDate checkIn, long nights) {
        PricingConfig pricingConfig = new PricingConfig();
        double total = 0.0;

        for (RoomType room : rooms) {
            double roomBasePrice = room.getBasePrice();
            LocalDate currentDate = checkIn;
            for (int i = 0; i < nights; i++) {
                double nightPrice = roomBasePrice;
                if (isWeekend(currentDate)) {
                    nightPrice *= pricingConfig.getWeekendMultiplier();
                } else {
                    nightPrice *= pricingConfig.getWeekdayMultiplier();
                }

                if (pricingConfig.isPeakSeason(currentDate)) {
                    nightPrice *= pricingConfig.getPeakSeasonMultiplier();
                }

                total += nightPrice;
                currentDate = currentDate.plusDays(1);
            }
        }

        return total;
    }

    private double calculateAddOnCharges(List<String> addOns, long nights) {
        if (addOns == null) return 0.0;

        double total = 0.0;
        Map<String, Double> addOnPrices = BillingContext.getAvailableAddOns();

        for (String addOn : addOns) {
            Double price = addOnPrices.get(addOn);
            if (price == null) continue;

            boolean perNight = BillingContext.isAddOnPerNight(addOn);
            total += perNight ? (price * nights) : price;
        }

        return total;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
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
        List<RoomType> managedRooms = attachManagedRooms(rooms);
        for (RoomType room : managedRooms) {
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
        reservationRepository.saveOrUpdate(reservation, managedRooms);
    }

    public void cancelReservation(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.saveOrUpdate(reservation, reservation.getRooms());
    }

    public List<Reservation> findReservationsForGuest(Guest guest) {
        return reservationRepository.findByGuest(guest);
    }

    /**
     * Find reservation by ID
     */
    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }

    public void applyDiscount(Long reservationId, double percent) {
        AdminUser currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No logged-in user when applying discount.");
        }

        double maxAllowed;
        switch (currentUser.getRole()) {
            case MANAGER:
                maxAllowed = 30.0;
                break;
            case ADMIN:
                maxAllowed = 15.0;
                break;
            default:
                maxAllowed = 0.0;
        }

        if (percent < 0) {
            throw new IllegalArgumentException("Discount percent cannot be negative.");
        }
        if (percent > maxAllowed) {
            throw new IllegalArgumentException(
                    "Discount " + percent + "% exceeds allowed cap of " + maxAllowed + "% for role " + currentUser.getRole());
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        double base = reservation.calculateBaseSubtotal();
        double discountAmount = base * (percent / 100.0);
        if (discountAmount > base) {
            throw new IllegalArgumentException("Discount would make subtotal negative.");
        }

        reservation.setDiscountPercent(percent);
        reservationRepository.saveOrUpdate(reservation, reservation.getRooms() != null ? reservation.getRooms() : new ArrayList<>());

        ActivityLogger.log(
                currentUser.getUsername(),
                "APPLY_DISCOUNT",
                "Reservation",
                reservation.getId() != null ? reservation.getId().toString() : "-",
                String.format("Applied %.2f%% discount. Base: %.2f, Discount: %.2f, New subtotal: %.2f",
                        percent,
                        base,
                        discountAmount,
                        base - discountAmount)
        );
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

    private List<RoomType> attachManagedRooms(List<RoomType> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return new ArrayList<>();
        }

        List<RoomType> managed = new ArrayList<>();
        for (RoomType room : rooms) {
            if (room == null) continue;

            RoomType managedRoom = null;
            if (room.getId() != null) {
                managedRoom = roomRepository.findById(room.getId()).orElse(null);
            }

            if (managedRoom == null && room.getType() != null) {
                managedRoom = roomRepository.findByType(room.getType()).orElse(null);
            }

            if (managedRoom == null) {
                throw new IllegalArgumentException("Unknown room type: " + room.getType());
            }

            managed.add(managedRoom);
        }

        return managed;
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
