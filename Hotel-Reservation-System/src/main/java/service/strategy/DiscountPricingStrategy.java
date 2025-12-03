package service.strategy;

public class DiscountPricingStrategy implements BillingStrategy {
    private final double discountRate;

    public DiscountPricingStrategy(double discountRate) {
        this.discountRate = discountRate;
    }

    @Override
    public double apply(double baseAmount) {
        return baseAmount * (1 - discountRate);
    }
}
