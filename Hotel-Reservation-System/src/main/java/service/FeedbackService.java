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

    public Feedback submitFeedback(String guestEmail, Long reservationId, int rating, String comments) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation not found.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        String reservationEmail = reservation.getGuest() != null ? reservation.getGuest().getEmail() : null;
        boolean emailsMatch = guestEmail != null
                && reservationEmail != null
                && reservationEmail.toLowerCase(Locale.ROOT).equals(guestEmail.toLowerCase(Locale.ROOT));

        if (!emailsMatch) {
            throw new IllegalArgumentException("Email does not match the reservation guest.");
        }

        Feedback feedback = new Feedback();
        feedback.setGuestEmail(guestEmail);
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
