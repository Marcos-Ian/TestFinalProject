package service;

import config.LoyaltyConfig;
import model.Guest;
import java.util.UUID;
/**
 * Provides loyalty point earning and redemption calculations.
 */
public class LoyaltyService {
    private final LoyaltyConfig loyaltyConfig;

    public LoyaltyService(LoyaltyConfig loyaltyConfig) {
        this.loyaltyConfig = loyaltyConfig;
    }

    public int calculateEarnedPoints(double amountSpent) {
        return loyaltyConfig.calculateEarnedPoints(amountSpent);
    }

    public double applyRedemption(double amount, int pointsToRedeem) {
        int redeemable = Math.min(pointsToRedeem, loyaltyConfig.getRedeemCap());
        double credit = redeemable / 100.0;
        return Math.max(0, amount - credit);
    }

    /**
     * Enroll a guest into the loyalty program and return a generated loyalty number.
     * This basic implementation simply generates and returns an identifier; persistence
     * can be added later when a loyalty repository exists.
     */
    public String enrollGuest(Guest guest) {
        return "LOY-" + UUID.randomUUID();
    }

    public LoyaltyConfig getLoyaltyConfig() {
        return loyaltyConfig;
    }
}
