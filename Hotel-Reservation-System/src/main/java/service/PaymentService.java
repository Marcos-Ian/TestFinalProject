package service;

import config.LoyaltyConfig;
import model.Guest;
import model.Payment;
import model.PaymentMethod;
import model.PaymentType;
import model.Reservation;
import model.ReservationAddOn;
import model.ReservationStatus;
import repository.GuestRepository;
import repository.PaymentRepository;
import repository.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PaymentService {
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final GuestRepository guestRepository;
    private final LoyaltyConfig loyaltyConfig;
    private final BillingContext billingContext;

    public PaymentService(ReservationRepository reservationRepository,
                          PaymentRepository paymentRepository,
                          GuestRepository guestRepository,
                          LoyaltyConfig loyaltyConfig,
                          BillingContext billingContext) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.guestRepository = guestRepository;
        this.loyaltyConfig = loyaltyConfig;
        this.billingContext = billingContext;
    }

    public BigDecimal calculateTotalPaid(Reservation reservation) {
        if (reservation == null) {
            return BigDecimal.ZERO;
        }

        List<Payment> payments = new ArrayList<>();
        if (reservation.getPayments() != null && !reservation.getPayments().isEmpty()) {
            payments.addAll(reservation.getPayments());
        } else if (reservation.getId() != null) {
            payments.addAll(paymentRepository.findByReservationId(reservation.getId()));
        }

        BigDecimal total = payments.stream()
                .map(this::normalizedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateBalance(Reservation reservation) {
        BigDecimal total = resolveReservationTotal(reservation);
        BigDecimal paid = calculateTotalPaid(reservation);
        BigDecimal balance = total.subtract(paid);
        return balance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public double applyLoyaltyRedemption(Guest guest, int pointsToRedeem) {
        if (pointsToRedeem <= 0) return 0.0;
        int available = guest.getLoyaltyPoints();
        if (pointsToRedeem > available) {
            throw new IllegalArgumentException("Guest does not have enough loyalty points.");
        }
        double discount = loyaltyConfig.convertPointsToDollars(pointsToRedeem);
        guest.setLoyaltyPoints(available - pointsToRedeem);
        return discount;
    }

    public double calculateFinalTotal(double baseSubtotal,
                                     double percentDiscount,
                                     double loyaltyDiscount) {
        double total = baseSubtotal;
        total -= loyaltyDiscount;
        double appliedDiscount = Math.max(0, Math.min(percentDiscount, 100));
        if (appliedDiscount > 0) {
            total -= total * (appliedDiscount / 100.0);
        }
        return Math.max(total, 0.0);
    }

    public void awardPointsForPayment(Guest guest, double amountPaid) {
        int earned = loyaltyConfig.calculateEarnedPoints(amountPaid);
        guest.setLoyaltyPoints(guest.getLoyaltyPoints() + earned);
    }

    public Payment processPayment(Long reservationId,
                                  double amount,
                                  PaymentMethod method,
                                  PaymentType type,
                                  double percentDiscount,
                                  int pointsToRedeem) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation not found");
        }
        if (method == null || type == null) {
            throw new IllegalArgumentException("Payment method and type are required.");
        }
        if (pointsToRedeem < 0) {
            throw new IllegalArgumentException("Loyalty redemption cannot be negative.");
        }
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        Guest guest = reservation.getGuest();
        if (guest == null) {
            throw new IllegalArgumentException("Reservation does not have an associated guest.");
        }
        double baseSubtotal = reservation.calculateBaseSubtotal();

        double loyaltyDiscount = applyLoyaltyRedemption(guest, pointsToRedeem);
        double finalTotal = calculateFinalTotal(baseSubtotal, percentDiscount, loyaltyDiscount);

        if (percentDiscount > 0) {
            reservation.setDiscountPercent(percentDiscount);
        }

        double alreadyPaid = calculateTotalPaid(reservation).doubleValue();
        double balanceBefore = finalTotal - alreadyPaid;

        if (type != PaymentType.REFUND && amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (type == PaymentType.NORMAL || type == PaymentType.DEPOSIT) {
            if (amount > balanceBefore + 0.01) {
                throw new IllegalArgumentException("Amount exceeds outstanding balance.");
            }
        }
        if (type == PaymentType.REFUND) {
            if (amount >= 0) {
                throw new IllegalArgumentException("Refund payments must be negative.");
            }
            double totalPaid = calculateTotalPaid(reservation).doubleValue();
            if (Math.abs(amount) > totalPaid + 0.01) {
                throw new IllegalArgumentException("Cannot refund more than was paid.");
            }
        }

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
        payment.setMethod(method);
        payment.setType(type);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setCreatedBy("System");

        if (reservation.getPayments() == null) {
            reservation.setPayments(new ArrayList<>());
        }
        reservation.getPayments().add(payment);
        reservation.setTotalAmount(finalTotal);

        if (amount > 0 && method != PaymentMethod.LOYALTY_POINTS) {
            awardPointsForPayment(guest, amount);
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
                reservation.getStatus() == ReservationStatus.COMPLETED) {
            double remaining = calculateBalance(reservation).doubleValue();
            if (remaining <= 0.01) {
                reservation.setFeedbackEligible(true);
            }
        }

        guestRepository.save(guest);
        reservationRepository.save(reservation);
        paymentRepository.save(payment);

        return payment;
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
            double discountPercent = reservation.getDiscountPercent();
            calculated -= calculated * (discountPercent / 100.0);
            return BigDecimal.valueOf(calculated);
        }

        double baseSubtotal = reservation.calculateBaseSubtotal();
        double discountPercent = reservation.getDiscountPercent();
        double total = baseSubtotal - (baseSubtotal * (discountPercent / 100.0));
        return BigDecimal.valueOf(total);
    }

    private BigDecimal normalizedAmount(Payment payment) {
        if (payment == null || payment.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = payment.getAmount();
        if (payment.getType() == PaymentType.REFUND && amount.signum() > 0) {
            amount = amount.negate();
        }
        return amount;
    }
}
