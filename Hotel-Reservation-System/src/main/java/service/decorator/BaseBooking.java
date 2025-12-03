package service.decorator;

public class BaseBooking implements AddOn {
    @Override
    public double cost(double baseAmount) {
        return baseAmount;
    }
}
