package service.strategy;

/**
 * Strategy abstraction for billing calculations.
 */
public interface BillingStrategy {
    double apply(double baseAmount);
}
