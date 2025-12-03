package factory;

import model.RoomType;

public class RoomFactory {
    public static RoomType create(RoomType.Type type, double basePrice, int capacity) {
        RoomType roomType = new RoomType();
        roomType.setType(type);
        roomType.setBasePrice(basePrice);
        roomType.setCapacity(capacity);
        return roomType;
    }
}
