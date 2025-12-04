package config;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration class for dynamic pricing rules.
 * Manages weekend/weekday multipliers and seasonal pricing.
 */
public class PricingConfig {
    private static final Logger LOGGER = Logger.getLogger(PricingConfig.class.getName());

    // Pricing multipliers
    private double weekendMultiplier = 1.2;  // 20% increase on weekends
    private double weekdayMultiplier = 1.0;  // Standard pricing on weekdays
    private double peakSeasonMultiplier = 1.5; // 50% increase during peak season

    // Peak season date ranges (month -> is peak)
    private final Map<Month, Boolean> peakSeasonMonths = new HashMap<>();

    // Tax rate
    private double taxRate = 0.10; // 10% tax

    public PricingConfig() {
        initializeDefaultPeakSeasons();
    }

    /**
     * Initialize default peak seasons
     * Typically summer (June-August) and winter holidays (December)
     */
    private void initializeDefaultPeakSeasons() {
        // Set all months to non-peak by default
        for (Month month : Month.values()) {
            peakSeasonMonths.put(month, false);
        }

        // Set peak season months
        peakSeasonMonths.put(Month.JUNE, true);
        peakSeasonMonths.put(Month.JULY, true);
        peakSeasonMonths.put(Month.AUGUST, true);
        peakSeasonMonths.put(Month.DECEMBER, true);

        LOGGER.info("Default peak seasons configured: June-August, December");
    }

    // Getters and Setters

    public double getWeekendMultiplier() {
        return weekendMultiplier;
    }

    public void setWeekendMultiplier(double weekendMultiplier) {
        if (weekendMultiplier < 1.0) {
            throw new IllegalArgumentException("Weekend multiplier must be >= 1.0");
        }
        this.weekendMultiplier = weekendMultiplier;
        LOGGER.info("Weekend multiplier set to: " + weekendMultiplier);
    }

    public double getWeekdayMultiplier() {
        return weekdayMultiplier;
    }

    public void setWeekdayMultiplier(double weekdayMultiplier) {
        if (weekdayMultiplier < 0.5) {
            throw new IllegalArgumentException("Weekday multiplier must be >= 0.5");
        }
        this.weekdayMultiplier = weekdayMultiplier;
        LOGGER.info("Weekday multiplier set to: " + weekdayMultiplier);
    }

    public double getPeakSeasonMultiplier() {
        return peakSeasonMultiplier;
    }

    public void setPeakSeasonMultiplier(double peakSeasonMultiplier) {
        if (peakSeasonMultiplier < 1.0) {
            throw new IllegalArgumentException("Peak season multiplier must be >= 1.0");
        }
        this.peakSeasonMultiplier = peakSeasonMultiplier;
        LOGGER.info("Peak season multiplier set to: " + peakSeasonMultiplier);
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        if (taxRate < 0 || taxRate > 1.0) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 1.0");
        }
        this.taxRate = taxRate;
        LOGGER.info("Tax rate set to: " + (taxRate * 100) + "%");
    }

    /**
     * Check if a given date is in peak season
     */
    public boolean isPeakSeason(LocalDate date) {
        return peakSeasonMonths.getOrDefault(date.getMonth(), false);
    }

    /**
     * Set whether a specific month is peak season
     */
    public void setPeakSeasonMonth(Month month, boolean isPeak) {
        peakSeasonMonths.put(month, isPeak);
        LOGGER.info(String.format("%s set to %s", month, isPeak ? "peak season" : "regular season"));
    }

    /**
     * Get all peak season months
     */
    public Map<Month, Boolean> getPeakSeasonMonths() {
        return new HashMap<>(peakSeasonMonths);
    }
}