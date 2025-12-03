package config;

/**
 * Configuration for loyalty point earning and redemption caps.
 */
public class LoyaltyConfig {
    private int earnRate = 10; // points per dollar
    private int redeemCap = 5000; // per reservation cap

    public int getEarnRate() {
        return earnRate;
    }

    public void setEarnRate(int earnRate) {
        this.earnRate = earnRate;
    }

    public int getRedeemCap() {
        return redeemCap;
    }

    public void setRedeemCap(int redeemCap) {
        this.redeemCap = redeemCap;
    }
}
