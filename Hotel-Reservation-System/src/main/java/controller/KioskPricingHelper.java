package controller;

import config.PricingConfig;
import model.RoomType;
import service.BillingContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for calculating billing breakdowns for kiosk displays without
 * modifying core billing logic.
 */
public final class KioskPricingHelper {
    private KioskPricingHelper() {
    }

    public static BookingBreakdown calculate(List<RoomType> rooms, List<String> addOns,
                                             LocalDate checkIn, LocalDate checkOut,
                                             BillingContext billingContext, PricingConfig pricingConfig) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double roomCharges = calculateRoomCharges(rooms, checkIn, nights, pricingConfig);
        double addOnCharges = calculateAddOnCharges(addOns, nights);
        double subtotal = roomCharges + addOnCharges;
        double afterStrategy = billingContext.getStrategy() != null ? billingContext.getStrategy().apply(subtotal) : subtotal;
        double tax = afterStrategy * 0.10;
        double total = afterStrategy + tax;
        return new BookingBreakdown(roomCharges, addOnCharges, subtotal, tax, total);
    }

    private static double calculateRoomCharges(List<RoomType> rooms, LocalDate checkIn, long nights, PricingConfig pricingConfig) {
        double total = 0.0;

        for (RoomType room : rooms) {
            double roomBasePrice = room.getBasePrice();
            LocalDate currentDate = checkIn;
            for (int i = 0; i < nights; i++) {
                double nightPrice = roomBasePrice;
                if (isWeekend(currentDate)) {
                    double weekendMultiplier = pricingConfig != null ? pricingConfig.getWeekendMultiplier() : 1.2;
                    nightPrice *= weekendMultiplier;
                } else if (pricingConfig != null) {
                    nightPrice *= pricingConfig.getWeekdayMultiplier();
                }

                if (pricingConfig != null && pricingConfig.isPeakSeason(currentDate)) {
                    nightPrice *= pricingConfig.getPeakSeasonMultiplier();
                }

                total += nightPrice;
                currentDate = currentDate.plusDays(1);
            }
        }

        return total;
    }

    private static double calculateAddOnCharges(List<String> addOns, long nights) {
        double total = 0.0;
        Map<String, Double> prices = BillingContext.getAvailableAddOns();
        for (String addOn : new ArrayList<>(addOns)) {
            Double price = prices.get(addOn);
            if (price == null) {
                continue;
            }

            boolean perNight = BillingContext.isAddOnPerNight(addOn);
            total += perNight ? price * nights : price;
        }
        return total;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
    }

    /**
     * Lightweight DTO for kiosk price displays.
     */
    public record BookingBreakdown(double roomSubtotal, double addOnSubtotal, double subtotal, double tax, double total) {
    }
}
