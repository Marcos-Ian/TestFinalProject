package service.strategy;

import config.PricingConfig;

/**
 * Default billing strategy that applies configured multipliers without discounts.
 */
public class StandardBillingStrategy implements BillingStrategy {
    private final PricingConfig pricingConfig;
    private final boolean weekend;
    private final boolean peakSeason;

    public StandardBillingStrategy(PricingConfig pricingConfig) {
        this(pricingConfig, false, false);
    }

    public StandardBillingStrategy(PricingConfig pricingConfig, boolean weekend, boolean peakSeason) {
        this.pricingConfig = pricingConfig;
        this.weekend = weekend;
        this.peakSeason = peakSeason;
    }

    @Override
    public double apply(double baseAmount) {
        double multiplier = weekend ? pricingConfig.getWeekendMultiplier() : pricingConfig.getWeekdayMultiplier();
        if (peakSeason) {
            multiplier *= pricingConfig.getPeakSeasonMultiplier();
        }
        return baseAmount * multiplier;
    }
}
