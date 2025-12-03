package service.decorator;

public abstract class AddOnDecorator implements AddOn {
    protected final AddOn delegate;
    protected final double addOnPrice;

    protected AddOnDecorator(AddOn delegate, double addOnPrice) {
        this.delegate = delegate;
        this.addOnPrice = addOnPrice;
    }

    @Override
    public double cost(double baseAmount) {
        return delegate.cost(baseAmount) + addOnPrice;
    }
}
