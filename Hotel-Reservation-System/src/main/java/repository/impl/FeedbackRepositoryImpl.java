package repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Feedback;
import repository.FeedbackRepository;

import java.util.List;

public class FeedbackRepositoryImpl implements FeedbackRepository {
    private final EntityManager entityManager;

    public FeedbackRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Feedback save(Feedback feedback) {
        entityManager.getTransaction().begin();
        if (feedback.getId() == null) {
            entityManager.persist(feedback);
        } else {
            feedback = entityManager.merge(feedback);
        }
        entityManager.getTransaction().commit();
        return feedback;
    }

    @Override
    public List<Feedback> findAll() {
        TypedQuery<Feedback> query = entityManager.createQuery("SELECT f FROM Feedback f", Feedback.class);
        return query.getResultList();
    }

    @Override
    public List<Feedback> findByGuestEmail(String guestEmail) {
        TypedQuery<Feedback> query = entityManager.createQuery(
                "SELECT f FROM Feedback f WHERE LOWER(f.guestEmail) = LOWER(:email)", Feedback.class);
        query.setParameter("email", guestEmail);
        return query.getResultList();
    }
}
