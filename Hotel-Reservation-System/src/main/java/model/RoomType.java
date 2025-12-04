package model;

import jakarta.persistence.*;

@Entity
public class RoomType {
    public enum Type { SINGLE, DOUBLE, DELUXE, PENTHOUSE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private Type type;
    private double basePrice;
    private int capacity;

    public Long getId() { return id; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}
