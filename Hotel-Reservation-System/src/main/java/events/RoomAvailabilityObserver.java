package events;

public interface RoomAvailabilityObserver {
    void onAvailabilityChanged(String roomType, int availableCount);
}
