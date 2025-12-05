package repository;

import model.Feedback;

import java.util.List;

public interface FeedbackRepository {
    Feedback save(Feedback feedback);

    List<Feedback> findAll();

    List<Feedback> findByGuestEmail(String guestEmail);
}
