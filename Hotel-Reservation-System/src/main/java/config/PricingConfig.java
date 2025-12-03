package config;

/**
 * Holds configurable pricing multipliers for weekend, weekday, and seasonal adjustments.
 */
public class PricingConfig {
    private double weekendMultiplier = 1.2;
    private double weekdayMultiplier = 1.0;
    private double peakSeasonMultiplier = 1.5;

    public double getWeekendMultiplier() {
        return weekendMultiplier;
    }

    public void setWeekendMultiplier(double weekendMultiplier) {
        this.weekendMultiplier = weekendMultiplier;
    }

    public double getWeekdayMultiplier() {
        return weekdayMultiplier;
    }

    public void setWeekdayMultiplier(double weekdayMultiplier) {
        this.weekdayMultiplier = weekdayMultiplier;
    }

    public double getPeakSeasonMultiplier() {
        return peakSeasonMultiplier;
    }

    public void setPeakSeasonMultiplier(double peakSeasonMultiplier) {
        this.peakSeasonMultiplier = peakSeasonMultiplier;
    }
}
