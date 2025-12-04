// ============================================================================
// RoomService.java - Enhanced with availability checking
// ============================================================================
package service;

import events.RoomAvailabilityObserver;
import events.RoomAvailabilitySubject;
import model.RoomType;
import repository.RoomRepository;
import repository.ReservationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages room availability and notifies observers when inventory changes.
 */
public class RoomService {
    private static final Logger LOGGER = Logger.getLogger(RoomService.class.getName());

    private final RoomAvailabilitySubject availabilitySubject;
    private final Map<String, Integer> availabilityByType = new HashMap<>();
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public RoomService(RoomAvailabilitySubject availabilitySubject) {
        this.availabilitySubject = availabilitySubject;
        this.roomRepository = null;
        this.reservationRepository = null;
        initializeDefaultAvailability();
    }

    public RoomService(RoomAvailabilitySubject availabilitySubject,
                       RoomRepository roomRepository,
                       ReservationRepository reservationRepository) {
        this.availabilitySubject = availabilitySubject;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        initializeDefaultAvailability();
    }

    /**
     * Initialize default room availability for each type
     */
    private void initializeDefaultAvailability() {
        availabilityByType.put("SINGLE", 10);
        availabilityByType.put("DOUBLE", 8);
        availabilityByType.put("DELUXE", 5);
        availabilityByType.put("PENTHOUSE", 2);
        LOGGER.info("Default room availability initialized");
    }

    public void registerObserver(RoomAvailabilityObserver observer) {
        availabilitySubject.addObserver(observer);
    }

    public void unregisterObserver(RoomAvailabilityObserver observer) {
        availabilitySubject.removeObserver(observer);
    }

    public void updateAvailability(String roomType, int availableCount) {
        int previousCount = availabilityByType.getOrDefault(roomType, 0);
        availabilityByType.put(roomType, availableCount);

        LOGGER.info(String.format("Room availability updated: %s from %d to %d",
                roomType, previousCount, availableCount));

        // Notify observers if availability increased (rooms became available)
        if (availableCount > previousCount) {
            availabilitySubject.notifyChange(roomType, availableCount);
        }
    }

    public int getAvailability(String roomType) {
        return availabilityByType.getOrDefault(roomType, 0);
    }

    /**
     * Get available rooms for a date range
     * Checks against existing reservations and returns available room types
     *
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return List of available RoomType objects
     */
    public List<RoomType> getAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        LOGGER.info(String.format("Checking room availability from %s to %s", checkIn, checkOut));

        List<RoomType> availableRooms = new ArrayList<>();

        // If repository is available, query from database
        if (roomRepository != null && reservationRepository != null) {
            availableRooms = queryAvailableRoomsFromDatabase(checkIn, checkOut);
        } else {
            // Otherwise, use in-memory availability
            availableRooms = getAvailableRoomsFromMemory();
        }

        LOGGER.info(String.format("Found %d available room types", availableRooms.size()));
        return availableRooms;
    }

    /**
     * Query available rooms from database considering existing reservations
     */
    private List<RoomType> queryAvailableRoomsFromDatabase(LocalDate checkIn, LocalDate checkOut) {
        // Get all room types
        List<RoomType> allRoomTypes = roomRepository.findAll();

        // For each room type, check if rooms are available
        List<RoomType> available = new ArrayList<>();
        for (RoomType roomType : allRoomTypes) {
            int totalRooms = availabilityByType.getOrDefault(roomType.getType().name(), 0);

            // Count booked rooms for this type in the date range
            int bookedRooms = reservationRepository.countBookedRooms(
                    roomType.getType().name(), checkIn, checkOut);

            int availableCount = totalRooms - bookedRooms;

            if (availableCount > 0) {
                available.add(roomType);
                LOGGER.fine(String.format("%s: %d available (%d total, %d booked)",
                        roomType.getType(), availableCount, totalRooms, bookedRooms));
            }
        }

        return available;
    }

    /**
     * Get available rooms from in-memory inventory
     */
    private List<RoomType> getAvailableRoomsFromMemory() {
        List<RoomType> available = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : availabilityByType.entrySet()) {
            if (entry.getValue() > 0) {
                RoomType roomType = createRoomType(entry.getKey());
                available.add(roomType);
            }
        }

        return available;
    }

    /**
     * Create a RoomType object with default properties
     */
    private RoomType createRoomType(String type) {
        RoomType roomType = new RoomType();
        roomType.setType(RoomType.Type.valueOf(type));

        // Set base prices and capacities according to business rules
        switch (type) {
            case "SINGLE":
                roomType.setBasePrice(100.0);
                roomType.setCapacity(2);
                break;
            case "DOUBLE":
                roomType.setBasePrice(150.0);
                roomType.setCapacity(4);
                break;
            case "DELUXE":
                roomType.setBasePrice(250.0);
                roomType.setCapacity(2);
                break;
            case "PENTHOUSE":
                roomType.setBasePrice(500.0);
                roomType.setCapacity(2);
                break;
        }

        return roomType;
    }

    /**
     * Suggest room combinations for a given guest count
     * Follows business rules for group bookings
     */
    public List<RoomSuggestion> suggestRooms(int adults, int children,
                                             LocalDate checkIn, LocalDate checkOut) {
        int totalGuests = adults + children;
        List<RoomSuggestion> suggestions = new ArrayList<>();

        LOGGER.info(String.format("Suggesting rooms for %d guests (%d adults, %d children)",
                totalGuests, adults, children));

        // Single guest - suggest single room
        if (totalGuests == 1) {
            suggestions.add(new RoomSuggestion("Single room", List.of("SINGLE")));
        }
        // 2 guests - single or double
        else if (totalGuests == 2) {
            suggestions.add(new RoomSuggestion("One single room", List.of("SINGLE")));
            suggestions.add(new RoomSuggestion("One double room", List.of("DOUBLE")));
        }
        // 3-4 guests - double or two singles
        else if (totalGuests >= 3 && totalGuests <= 4) {
            suggestions.add(new RoomSuggestion("One double room", List.of("DOUBLE")));
            suggestions.add(new RoomSuggestion("Two single rooms", List.of("SINGLE", "SINGLE")));
        }
        // More than 4 guests - multiple rooms
        else {
            int remainingGuests = totalGuests;
            List<String> rooms = new ArrayList<>();

            // Fill with double rooms first (4 guests each)
            while (remainingGuests > 4) {
                rooms.add("DOUBLE");
                remainingGuests -= 4;
            }

            // Handle remaining guests
            if (remainingGuests > 2) {
                rooms.add("DOUBLE");
            } else if (remainingGuests > 0) {
                rooms.add("SINGLE");
            }

            suggestions.add(new RoomSuggestion(
                    String.format("%d double rooms", rooms.size()), rooms));
        }

        return suggestions;
    }

    /**
     * Validate room selection against occupancy rules
     */
    public boolean validateOccupancy(List<RoomType> rooms, int adults, int children) {
        int totalGuests = adults + children;
        int totalCapacity = rooms.stream()
                .mapToInt(RoomType::getCapacity)
                .sum();

        boolean valid = totalCapacity >= totalGuests;

        if (!valid) {
            LOGGER.warning(String.format("Occupancy validation failed: %d guests, %d capacity",
                    totalGuests, totalCapacity));
        }

        return valid;
    }

    /**
     * Mark rooms as booked (reduce availability)
     */
    public void bookRooms(List<RoomType> rooms) {
        for (RoomType room : rooms) {
            String type = room.getType().name();
            int current = availabilityByType.getOrDefault(type, 0);
            if (current > 0) {
                updateAvailability(type, current - 1);
            }
        }
    }

    /**
     * Mark rooms as available (increase availability after checkout)
     */
    public void releaseRooms(List<RoomType> rooms) {
        for (RoomType room : rooms) {
            String type = room.getType().name();
            int current = availabilityByType.getOrDefault(type, 0);
            updateAvailability(type, current + 1);
        }
    }
}

/**
 * Data class for room suggestions
 */
