package service;

import events.RoomAvailabilityObserver;
import events.RoomAvailabilitySubject;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages room availability and notifies observers when inventory changes.
 */
public class RoomService {
    private final RoomAvailabilitySubject availabilitySubject;
    private final Map<String, Integer> availabilityByType = new HashMap<>();

    public RoomService(RoomAvailabilitySubject availabilitySubject) {
        this.availabilitySubject = availabilitySubject;
    }

    public void registerObserver(RoomAvailabilityObserver observer) {
        availabilitySubject.addObserver(observer);
    }

    public void unregisterObserver(RoomAvailabilityObserver observer) {
        availabilitySubject.removeObserver(observer);
    }

    public void updateAvailability(String roomType, int availableCount) {
        availabilityByType.put(roomType, availableCount);
        availabilitySubject.notifyChange(roomType, availableCount);
    }

    public int getAvailability(String roomType) {
        return availabilityByType.getOrDefault(roomType, 0);
    }
}
