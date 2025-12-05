package service;

import model.Feedback;
import model.Guest;
import model.ReservationStatus;
import repository.FeedbackRepository;
import repository.GuestRepository;
import repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

public class FeedbackService {
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(GuestRepository guestRepository,
                           ReservationRepository reservationRepository,
                           FeedbackRepository feedbackRepository) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback submitFeedback(String guestEmail, int rating, String comments) {
        if (guestEmail == null || guestEmail.isBlank()) {
            throw new IllegalArgumentException("Guest email is required for feedback");
        }

        Guest guest = guestRepository.findByEmail(guestEmail)
                .orElseThrow(() -> new IllegalArgumentException("No guest found with email: " + guestEmail));

        boolean hasReservation = reservationRepository.existsByGuestAndStatus(guest, ReservationStatus.CHECKED_OUT)
                || reservationRepository.existsByGuestAndStatus(guest, ReservationStatus.COMPLETED)
                || reservationRepository.existsByGuest(guest);

        if (!hasReservation) {
            throw new IllegalStateException("Guest with email " + guestEmail +
                    " has no completed reservation – feedback is only allowed for real stays.");
        }

        Feedback feedback = new Feedback();
        feedback.setGuestEmail(guestEmail);
        feedback.setRating(rating);
        feedback.setComments(comments);
        feedback.setCreatedAt(LocalDateTime.now());

        return feedbackRepository.save(feedback);
    }

    public List<Feedback> listFeedback() {
        return feedbackRepository.findAll();
    }
}
