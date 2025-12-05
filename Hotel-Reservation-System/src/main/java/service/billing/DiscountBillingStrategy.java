package service.billing;

import service.strategy.BillingStrategy;

/**
 * Billing strategy that applies a percentage discount before loyalty logic.
 */
public class DiscountBillingStrategy implements BillingStrategy {

    private final BillingStrategy delegate;
    private final double discountPercent;

    public DiscountBillingStrategy(BillingStrategy delegate, double discountPercent) {
        this.delegate = delegate;
        this.discountPercent = discountPercent;
    }

    @Override
    public double apply(double baseAmount) {
        double base = delegate != null ? delegate.apply(baseAmount) : baseAmount;

        double percent = Math.max(0, Math.min(discountPercent, 100));
        double discountAmount = base * (percent / 100.0);
        double discountedSubtotal = base - discountAmount;

        return Math.max(discountedSubtotal, 0.0);
    }
}
