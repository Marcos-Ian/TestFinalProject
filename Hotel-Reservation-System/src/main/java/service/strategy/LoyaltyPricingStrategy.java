package service.strategy;

import config.LoyaltyConfig;

public class LoyaltyPricingStrategy implements BillingStrategy {
    private final LoyaltyConfig config;
    private final int pointsToRedeem;

    public LoyaltyPricingStrategy(LoyaltyConfig config, int pointsToRedeem) {
        this.config = config;
        this.pointsToRedeem = pointsToRedeem;
    }

    @Override
    public double apply(double baseAmount) {
        double maxRedeemable = config.getRedeemCap();
        double credit = Math.min(pointsToRedeem, maxRedeemable) / 100.0;
        return Math.max(0, baseAmount - credit);
    }
}
