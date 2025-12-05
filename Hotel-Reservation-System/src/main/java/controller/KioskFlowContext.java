package controller;

import model.Guest;
import model.RoomType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple in-memory context that carries booking information across the kiosk screens.
 * This keeps the presentation layer stateful without touching backend services.
 */
public class KioskFlowContext {
    private static final KioskFlowContext INSTANCE = new KioskFlowContext();

    private LocalDate checkIn;
    private LocalDate checkOut;
    private int adults;
    private int children;
    private Guest guest = new Guest();
    private final List<RoomType> selectedRooms = new ArrayList<>();
    private final List<RoomType> suggestedRooms = new ArrayList<>();
    private boolean usingSuggestedPlan = true;
    private final List<String> addOns = new ArrayList<>();
    private double estimatedTotal;
    private double roomSubtotal;
    private double addOnSubtotal;
    private double tax;
    private Long lastReservationId;

    private KioskFlowContext() {
    }

    public static KioskFlowContext getInstance() {
        return INSTANCE;
    }

    public void reset() {
        checkIn = null;
        checkOut = null;
        adults = 1;
        children = 0;
        guest = new Guest();
        selectedRooms.clear();
        suggestedRooms.clear();
        usingSuggestedPlan = true;
        addOns.clear();
        estimatedTotal = 0.0;
        roomSubtotal = 0.0;
        addOnSubtotal = 0.0;
        tax = 0.0;
        lastReservationId = null;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public int getAdults() {
        return adults;
    }

    public void setAdults(int adults) {
        this.adults = adults;
    }

    public int getChildren() {
        return children;
    }

    public void setChildren(int children) {
        this.children = children;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public void setSelectedRooms(List<RoomType> rooms) {
        selectedRooms.clear();
        selectedRooms.addAll(rooms);
    }

    public List<RoomType> getSelectedRooms() {
        return selectedRooms;
    }

    public List<RoomType> getSuggestedRooms() {
        return suggestedRooms;
    }

    public void setSuggestedRooms(List<RoomType> rooms) {
        suggestedRooms.clear();
        suggestedRooms.addAll(rooms);
    }

    public boolean isUsingSuggestedPlan() {
        return usingSuggestedPlan;
    }

    public void setUsingSuggestedPlan(boolean usingSuggestedPlan) {
        this.usingSuggestedPlan = usingSuggestedPlan;
    }

    public List<String> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<String> addOns) {
        this.addOns.clear();
        this.addOns.addAll(addOns);
    }

    public double getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(double estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public double getRoomSubtotal() {
        return roomSubtotal;
    }

    public void setRoomSubtotal(double roomSubtotal) {
        this.roomSubtotal = roomSubtotal;
    }

    public double getAddOnSubtotal() {
        return addOnSubtotal;
    }

    public void setAddOnSubtotal(double addOnSubtotal) {
        this.addOnSubtotal = addOnSubtotal;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public Long getLastReservationId() {
        return lastReservationId;
    }

    public void setLastReservationId(Long lastReservationId) {
        this.lastReservationId = lastReservationId;
    }
}
