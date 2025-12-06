package repository;

import model.Payment;

import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);

    List<Payment> findByReservationId(Long reservationId);
}
