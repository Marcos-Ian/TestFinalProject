// Replace the existing Reservation.java with this updated version
package model;

import config.PricingConfig;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import model.Payment;

@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    private LocalDate checkIn;
    private LocalDate checkOut;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(name = "feedback_eligible")
    private boolean feedbackEligible;

    @Column(name = "discount_percent")
    private Double discountPercent = 0.0;

    // NEW: Store the calculated total amount
    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @ManyToMany
    @JoinTable(name = "reservation_room",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "room_type_id"))
    private List<RoomType> rooms = new ArrayList<>();

    // NEW: Relationship to add-ons
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReservationAddOn> addOns = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    public Long getId() { return id; }
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public List<RoomType> getRooms() { return rooms; }
    public void setRooms(List<RoomType> rooms) { this.rooms = rooms; }

    public Double getDiscountPercent() {
        return discountPercent != null ? discountPercent : 0.0;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }

    // NEW: Getters and setters for totalAmount
    public Double getTotalAmount() {
        return totalAmount != null ? totalAmount : 0.0;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    // NEW: Getters and setters for addOns
    public List<ReservationAddOn> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<ReservationAddOn> addOns) {
        this.addOns = addOns;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public boolean isFeedbackEligible() {
        return feedbackEligible;
    }

    public void setFeedbackEligible(boolean feedbackEligible) {
        this.feedbackEligible = feedbackEligible;
    }

    /**
     * Calculate room subtotal without discounts or loyalty.
     */
    public double calculateRoomSubtotal() {
        if (checkIn == null || checkOut == null || rooms == null || rooms.isEmpty()) {
            return 0.0;
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            return 0.0;
        }

        PricingConfig pricingConfig = new PricingConfig();
        double total = 0.0;

        for (RoomType room : rooms) {
            double basePrice = room.getBasePrice();
            LocalDate currentDate = checkIn;
            for (int i = 0; i < nights; i++) {
                double nightly = basePrice;
                if (isWeekend(currentDate)) {
                    nightly *= pricingConfig.getWeekendMultiplier();
                } else {
                    nightly *= pricingConfig.getWeekdayMultiplier();
                }

                if (pricingConfig.isPeakSeason(currentDate)) {
                    nightly *= pricingConfig.getPeakSeasonMultiplier();
                }

                total += nightly;
                currentDate = currentDate.plusDays(1);
            }
        }

        return total;
    }

    public double calculateAddonSubtotal() {
        if (addOns == null || addOns.isEmpty() || checkIn == null || checkOut == null) {
            return 0.0;
        }

        long nights = Math.max(0, ChronoUnit.DAYS.between(checkIn, checkOut));
        return addOns.stream().mapToDouble(addOn -> {
            double base = addOn.getPrice();
            return addOn.isPerNight() ? base * nights : base;
        }).sum();
    }

    /**
     * Base subtotal before any discounts or loyalty, including add-ons.
     */
    public double calculateBaseSubtotal() {
        return calculateRoomSubtotal() + calculateAddonSubtotal();
    }

    public double getTotalPaid() {
        return payments == null ? 0.0 :
                payments.stream()
                        .filter(payment -> payment.getAmount() != null)
                        .mapToDouble(payment -> payment.getAmount().doubleValue())
                        .sum();
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}