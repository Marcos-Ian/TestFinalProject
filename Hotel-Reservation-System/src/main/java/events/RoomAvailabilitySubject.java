package events;

import java.util.ArrayList;
import java.util.List;

public class RoomAvailabilitySubject {
    private final List<RoomAvailabilityObserver> observers = new ArrayList<>();

    public void addObserver(RoomAvailabilityObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(RoomAvailabilityObserver observer) {
        observers.remove(observer);
    }

    public void notifyChange(String roomType, int availableCount) {
        observers.forEach(o -> o.onAvailabilityChanged(roomType, availableCount));
    }
}
