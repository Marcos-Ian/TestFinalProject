package service.decorator;

public class BreakfastAddOn extends AddOnDecorator {
    public BreakfastAddOn(AddOn delegate, double addOnPrice) {
        super(delegate, addOnPrice);
    }
}
