package service;

import java.util.List;

public class RoomSuggestion {
    private final String description;
    private final List<String> roomTypes;

    public RoomSuggestion(String description, List<String> roomTypes) {
        this.description = description;
        this.roomTypes = roomTypes;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getRoomTypes() {
        return roomTypes;
    }
}
