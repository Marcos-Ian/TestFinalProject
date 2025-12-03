package service;

import service.strategy.BillingStrategy;

/**
 * Context for applying different billing strategies (standard, discount, loyalty).
 */
public class BillingContext {
    private BillingStrategy strategy;

    public void setStrategy(BillingStrategy strategy) {
        this.strategy = strategy;
    }

    public double calculate(double baseAmount) {
        if (strategy == null) {
            throw new IllegalStateException("Billing strategy not configured");
        }
        return strategy.apply(baseAmount);
    }
}
