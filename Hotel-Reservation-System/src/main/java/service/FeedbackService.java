package service;

import model.Feedback;
import model.Reservation;
import repository.FeedbackRepository;
import repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class FeedbackService {
    private final ReservationRepository reservationRepository;
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(ReservationRepository reservationRepository,
                           FeedbackRepository feedbackRepository) {
        this.reservationRepository = reservationRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback submitFeedback(String guestEmail, int rating, String comments) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        if (guestEmail == null || guestEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required to submit feedback.");
        }

        String normalizedEmail = guestEmail.trim().toLowerCase(Locale.ROOT);

        Reservation reservation = reservationRepository.findMostRecentReservationByGuestEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("No reservation found for this email."));

        String reservationEmail = reservation.getGuest() != null ? reservation.getGuest().getEmail() : null;
        boolean emailsMatch = reservationEmail != null
                && reservationEmail.trim().toLowerCase(Locale.ROOT).equals(normalizedEmail);

        if (!emailsMatch) {
            throw new IllegalArgumentException("Email does not match the reservation guest.");
        }

        double subtotal = reservation.getTotalAmount() != null && reservation.getTotalAmount() > 0
                ? reservation.getTotalAmount()
                : reservation.calculateBaseSubtotal();
        double discountPercent = reservation.getDiscountPercent();
        subtotal -= subtotal * (discountPercent / 100.0);
        double remaining = subtotal - reservation.getTotalPaid();

        if (remaining > 0.01 || !reservation.isFeedbackEligible()) {
            throw new IllegalArgumentException("Feedback can only be submitted after checkout with a zero balance.");
        }

        Feedback feedback = new Feedback();
        feedback.setGuestEmail(guestEmail != null ? guestEmail.trim() : null);
        feedback.setReservation(reservation);
        feedback.setRating(rating);
        feedback.setComments(comments);
        feedback.setCreatedAt(LocalDateTime.now());

        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }
}
