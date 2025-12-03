package service;

import config.LoyaltyConfig;

/**
 * Provides loyalty point earning and redemption calculations.
 */
public class LoyaltyService {
    private final LoyaltyConfig loyaltyConfig;

    public LoyaltyService(LoyaltyConfig loyaltyConfig) {
        this.loyaltyConfig = loyaltyConfig;
    }

    public int calculateEarnedPoints(double amountSpent) {
        return (int) Math.floor(amountSpent * loyaltyConfig.getEarnRate());
    }

    public double applyRedemption(double amount, int pointsToRedeem) {
        int redeemable = Math.min(pointsToRedeem, loyaltyConfig.getRedeemCap());
        double credit = redeemable / 100.0;
        return Math.max(0, amount - credit);
    }

    public LoyaltyConfig getLoyaltyConfig() {
        return loyaltyConfig;
    }
}
