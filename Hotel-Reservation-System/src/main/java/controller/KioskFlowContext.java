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
    private final List<String> addOns = new ArrayList<>();
    private double estimatedTotal;

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
        addOns.clear();
        estimatedTotal = 0.0;
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

    public List<RoomType> getSelectedRooms() {
        return selectedRooms;
    }

    public List<String> getAddOns() {
        return addOns;
    }

    public double getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(double estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }
}
