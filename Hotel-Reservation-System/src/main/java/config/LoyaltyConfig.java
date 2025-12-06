// ============================================================================
// LoyaltyConfig.java - Configuration for loyalty program
// ============================================================================
package config;

import java.util.logging.Logger;

/**
 * Configuration class for loyalty program rules.
 * Manages point earning rates and redemption caps.
 */
public class LoyaltyConfig {
    private static final Logger LOGGER = Logger.getLogger(LoyaltyConfig.class.getName());

    // Points earning rate (points per dollar spent)
    private double earnRate = 0.1; // default: 0.1 point per $1 (1 point per $10)

    // 1 point per $10
    private final double earnRatePerDollar = 0.1; // points per $1
    // 100 points => $1
    private final int pointsPerDollar = 100;

    // Points redemption rate (dollars per point)
    private double redeemRate = 0.01; // $0.01 per point (100 points = $1)

    // Maximum points that can be redeemed per reservation
    private int redeemCap = 5000; // Max 5000 points = $50 discount

    // Minimum points required to start redeeming
    private int minimumRedeemPoints = 100;

    public LoyaltyConfig() {
        LOGGER.info("LoyaltyConfig initialized with default values");
    }

    // Getters and Setters

    public double getEarnRate() {
        return earnRate;
    }

    public void setEarnRate(double earnRate) {
        if (earnRate <= 0) {
            throw new IllegalArgumentException("Earn rate must be positive");
        }
        this.earnRate = earnRate;
        LOGGER.info("Loyalty earn rate set to: " + earnRate + " points per $1");
    }

    public double getEarnRatePerDollar() { return earnRatePerDollar; }
    public int getPointsPerDollar() { return pointsPerDollar; }

    public double getRedeemRate() {
        return redeemRate;
    }

    public void setRedeemRate(double redeemRate) {
        if (redeemRate <= 0) {
            throw new IllegalArgumentException("Redeem rate must be positive");
        }
        this.redeemRate = redeemRate;
        LOGGER.info("Loyalty redeem rate set to: $" + redeemRate + " per point");
    }

    public int getRedeemCap() {
        return redeemCap;
    }

    public void setRedeemCap(int redeemCap) {
        if (redeemCap < 0) {
            throw new IllegalArgumentException("Redeem cap cannot be negative");
        }
        this.redeemCap = redeemCap;
        LOGGER.info("Loyalty redeem cap set to: " + redeemCap + " points");
    }

    public int getMinimumRedeemPoints() {
        return minimumRedeemPoints;
    }

    public void setMinimumRedeemPoints(int minimumRedeemPoints) {
        if (minimumRedeemPoints < 0) {
            throw new IllegalArgumentException("Minimum redeem points cannot be negative");
        }
        this.minimumRedeemPoints = minimumRedeemPoints;
        LOGGER.info("Minimum redeem points set to: " + minimumRedeemPoints);
    }

    /**
     * Calculate points earned from an amount
     */
    public int calculatePointsEarned(double amount) {
        return (int) (amount * earnRate);
    }

    public int calculateEarnedPoints(double amountPaid) {
        // floor so we don't over-issue points
        return (int) Math.floor(amountPaid * earnRatePerDollar);
    }

    /**
     * Calculate discount from points redeemed
     */
    public double calculateDiscount(int points) {
        if (points < minimumRedeemPoints) {
            return 0.0;
        }
        int pointsToRedeem = Math.min(points, redeemCap);
        return pointsToRedeem * redeemRate;
    }

    public double convertPointsToDollars(int points) {
        return points / (double) pointsPerDollar;
    }

    /**
     * Check if points can be redeemed
     */
    public boolean canRedeem(int points) {
        return points >= minimumRedeemPoints;
    }
}
