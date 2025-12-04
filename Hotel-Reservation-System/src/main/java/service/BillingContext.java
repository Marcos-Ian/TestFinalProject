// ============================================================================
// BillingContext.java - Enhanced with full calculation capabilities
// ============================================================================
package service;

import config.PricingConfig;
import model.RoomType;
import service.strategy.BillingStrategy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Context for applying different billing strategies (standard, discount, loyalty).
 * Enhanced to calculate complete reservation totals including rooms, add-ons, and pricing rules.
 */
public class BillingContext {
    private static final Logger LOGGER = Logger.getLogger(BillingContext.class.getName());

    private BillingStrategy strategy;
    private PricingConfig pricingConfig;

    // Add-on pricing (per night or per reservation)
    private static final Map<String, Double> ADD_ON_PRICES = new HashMap<>();
    private static final Map<String, Boolean> ADD_ON_PER_NIGHT = new HashMap<>();

    static {
        // Initialize add-on prices
        ADD_ON_PRICES.put("WiFi", 10.0);
        ADD_ON_PRICES.put("Breakfast", 25.0);
        ADD_ON_PRICES.put("Parking", 15.0);
        ADD_ON_PRICES.put("Spa", 100.0);

        // Define pricing model (true = per night, false = per reservation)
        ADD_ON_PER_NIGHT.put("WiFi", true);
        ADD_ON_PER_NIGHT.put("Breakfast", true);
        ADD_ON_PER_NIGHT.put("Parking", true);
        ADD_ON_PER_NIGHT.put("Spa", false);
    }

    public BillingContext() {
        // Default constructor
    }

    public BillingContext(PricingConfig pricingConfig) {
        this.pricingConfig = pricingConfig;
    }

    public void setStrategy(BillingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setPricingConfig(PricingConfig pricingConfig) {
        this.pricingConfig = pricingConfig;
    }

    /**
     * Apply strategy to a base amount
     */
    public double calculate(double baseAmount) {
        if (strategy == null) {
            throw new IllegalStateException("Billing strategy not configured");
        }
        return strategy.apply(baseAmount);
    }

    /**
     * Calculate complete reservation total including rooms, add-ons, tax, and pricing rules
     *
     * @param rooms List of selected room types
     * @param addOns List of selected add-on service names
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Total amount including all charges
     */
    public double calculateTotal(List<RoomType> rooms, List<String> addOns,
                                 LocalDate checkIn, LocalDate checkOut) {
        LOGGER.info(String.format("Calculating total for %d rooms, %d add-ons from %s to %s",
                rooms.size(), addOns.size(), checkIn, checkOut));

        // Calculate number of nights
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        // Calculate room charges with dynamic pricing
        double roomTotal = calculateRoomCharges(rooms, checkIn, checkOut, nights);

        // Calculate add-on charges
        double addOnTotal = calculateAddOnCharges(addOns, nights);

        // Subtotal before tax
        double subtotal = roomTotal + addOnTotal;

        // Apply billing strategy (discounts, loyalty, etc.)
        double afterStrategy = strategy != null ? strategy.apply(subtotal) : subtotal;

        // Add tax (10% standard rate)
        double tax = afterStrategy * 0.10;

        // Final total
        double total = afterStrategy + tax;

        LOGGER.info(String.format("Billing breakdown - Rooms: $%.2f, Add-ons: $%.2f, " +
                        "Subtotal: $%.2f, After strategy: $%.2f, Tax: $%.2f, Total: $%.2f",
                roomTotal, addOnTotal, subtotal, afterStrategy, tax, total));

        return total;
    }

    /**
     * Calculate room charges with dynamic pricing (weekends, peak season)
     */
    private double calculateRoomCharges(List<RoomType> rooms, LocalDate checkIn,
                                        LocalDate checkOut, long nights) {
        double total = 0.0;

        for (RoomType room : rooms) {
            double roomBasePrice = room.getBasePrice();

            // Calculate price for each night with multipliers
            LocalDate currentDate = checkIn;
            for (int i = 0; i < nights; i++) {
                double nightPrice = roomBasePrice;

                // Apply weekend multiplier
                if (isWeekend(currentDate)) {
                    double weekendMultiplier = pricingConfig != null ?
                            pricingConfig.getWeekendMultiplier() : 1.2;
                    nightPrice *= weekendMultiplier;
                    LOGGER.fine(String.format("Weekend multiplier applied: %.2fx", weekendMultiplier));
                } else {
                    double weekdayMultiplier = pricingConfig != null ?
                            pricingConfig.getWeekdayMultiplier() : 1.0;
                    nightPrice *= weekdayMultiplier;
                }

                // Apply peak season multiplier if configured
                if (pricingConfig != null && pricingConfig.isPeakSeason(currentDate)) {
                    nightPrice *= pricingConfig.getPeakSeasonMultiplier();
                    LOGGER.fine(String.format("Peak season multiplier applied: %.2fx",
                            pricingConfig.getPeakSeasonMultiplier()));
                }

                total += nightPrice;
                currentDate = currentDate.plusDays(1);
            }
        }

        return total;
    }

    /**
     * Calculate add-on service charges
     */
    private double calculateAddOnCharges(List<String> addOns, long nights) {
        double total = 0.0;

        for (String addOn : addOns) {
            Double price = ADD_ON_PRICES.get(addOn);
            if (price == null) {
                LOGGER.warning("Unknown add-on service: " + addOn);
                continue;
            }

            // Check if priced per night or per reservation
            boolean perNight = ADD_ON_PER_NIGHT.getOrDefault(addOn, false);
            double addOnCharge = perNight ? (price * nights) : price;

            total += addOnCharge;
            LOGGER.fine(String.format("Add-on '%s': $%.2f %s", addOn, addOnCharge,
                    perNight ? "(per night)" : "(per reservation)"));
        }

        return total;
    }

    /**
     * Calculate detailed breakdown for display
     */
    public BillingBreakdown calculateBreakdown(List<RoomType> rooms, List<String> addOns,
                                               LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        double roomTotal = calculateRoomCharges(rooms, checkIn, checkOut, nights);
        double addOnTotal = calculateAddOnCharges(addOns, nights);
        double subtotal = roomTotal + addOnTotal;
        double afterStrategy = strategy != null ? strategy.apply(subtotal) : subtotal;
        double tax = afterStrategy * 0.10;
        double total = afterStrategy + tax;

        return new BillingBreakdown(roomTotal, addOnTotal, subtotal,
                afterStrategy - subtotal, tax, total);
    }

    /**
     * Check if date is weekend (Saturday or Sunday)
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Get price for a specific add-on
     */
    public static double getAddOnPrice(String addOn) {
        return ADD_ON_PRICES.getOrDefault(addOn, 0.0);
    }

    /**
     * Check if add-on is priced per night
     */
    public static boolean isAddOnPerNight(String addOn) {
        return ADD_ON_PER_NIGHT.getOrDefault(addOn, false);
    }

    /**
     * Get all available add-ons
     */
    public static Map<String, Double> getAvailableAddOns() {
        return new HashMap<>(ADD_ON_PRICES);
    }
}

/**
 * Data class for billing breakdown
 */
class BillingBreakdown {
    private final double roomCharges;
    private final double addOnCharges;
    private final double subtotal;
    private final double discount;
    private final double tax;
    private final double total;

    public BillingBreakdown(double roomCharges, double addOnCharges, double subtotal,
                            double discount, double tax, double total) {
        this.roomCharges = roomCharges;
        this.addOnCharges = addOnCharges;
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
    }

    public double getRoomCharges() { return roomCharges; }
    public double getAddOnCharges() { return addOnCharges; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public double getTax() { return tax; }
    public double getTotal() { return total; }
}