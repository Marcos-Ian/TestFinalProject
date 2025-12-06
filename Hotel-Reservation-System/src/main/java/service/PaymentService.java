package service;

import model.Payment;
import model.PaymentMethod;
import model.PaymentType;
import model.Reservation;
import model.ReservationAddOn;
import repository.PaymentRepository;
import repository.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PaymentService {
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final BillingContext billingContext;
    private final LoyaltyService loyaltyService;

    public PaymentService(ReservationRepository reservationRepository,
                          PaymentRepository paymentRepository,
                          BillingContext billingContext,
                          LoyaltyService loyaltyService) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.billingContext = billingContext;
        this.loyaltyService = loyaltyService;
    }

    public BigDecimal calculateTotalPaid(Reservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return BigDecimal.ZERO;
        }
        List<Payment> payments = paymentRepository.findByReservationId(reservation.getId());
        return payments.stream()
                .map(this::normalizeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateBalance(Reservation reservation) {
        BigDecimal total = resolveReservationTotal(reservation);
        BigDecimal paid = calculateTotalPaid(reservation);
        BigDecimal balance = total.subtract(paid);
        return balance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public void processPayment(Long reservationId,
                               PaymentMethod method,
                               PaymentType type,
                               BigDecimal amount) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID is required");
        }
        if (method == null || type == null) {
            throw new IllegalArgumentException("Payment method and type are required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setMethod(method);
        payment.setType(type);
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setCreatedBy("System");

        reservation.getPayments().add(payment);
        paymentRepository.save(payment);
    }

    public Optional<Reservation> findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId);
    }

    public List<Payment> getPaymentsForReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }

    private BigDecimal resolveReservationTotal(Reservation reservation) {
        if (reservation == null) {
            return BigDecimal.ZERO;
        }

        Double totalAmount = reservation.getTotalAmount();
        if (totalAmount != null && totalAmount > 0) {
            return BigDecimal.valueOf(totalAmount);
        }

        if (billingContext != null && reservation.getRooms() != null && reservation.getCheckIn() != null && reservation.getCheckOut() != null) {
            List<String> addOns = reservation.getAddOns() != null
                    ? reservation.getAddOns().stream().map(ReservationAddOn::getAddOnName).collect(Collectors.toList())
                    : List.of();
            double calculated = billingContext.calculateTotal(reservation.getRooms(), addOns, reservation.getCheckIn(), reservation.getCheckOut());
            return BigDecimal.valueOf(calculated);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal normalizeAmount(Payment payment) {
        if (payment.getType() == PaymentType.REFUND) {
            return payment.getAmount().negate();
        }
        return payment.getAmount();
    }
}
