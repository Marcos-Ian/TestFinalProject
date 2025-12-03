package service.strategy;

public class StandardPricingStrategy implements BillingStrategy {
    @Override
    public double apply(double baseAmount) {
        return baseAmount;
    }
}
