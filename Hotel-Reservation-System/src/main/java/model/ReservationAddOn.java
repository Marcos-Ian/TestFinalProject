// Save this as: Hotel-Reservation-System/src/main/java/model/ReservationAddOn.java
package model;

import jakarta.persistence.*;

@Entity
@Table(name = "reservation_addon")
public class ReservationAddOn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(nullable = false)
    private String addOnName;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private boolean perNight;

    public ReservationAddOn() {}

    public ReservationAddOn(Reservation reservation, String addOnName, double price, boolean perNight) {
        this.reservation = reservation;
        this.addOnName = addOnName;
        this.price = price;
        this.perNight = perNight;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public String getAddOnName() { return addOnName; }
    public void setAddOnName(String addOnName) { this.addOnName = addOnName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isPerNight() { return perNight; }
    public void setPerNight(boolean perNight) { this.perNight = perNight; }
}