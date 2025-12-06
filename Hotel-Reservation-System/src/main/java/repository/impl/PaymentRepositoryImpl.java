package repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Payment;
import repository.PaymentRepository;

import java.util.List;

public class PaymentRepositoryImpl implements PaymentRepository {
    private final EntityManager entityManager;

    public PaymentRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Payment save(Payment payment) {
        entityManager.getTransaction().begin();
        try {
            if (payment.getId() == null) {
                entityManager.persist(payment);
            } else {
                payment = entityManager.merge(payment);
            }
            entityManager.getTransaction().commit();
            return payment;
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save payment", ex);
        }
    }

    @Override
    public List<Payment> findByReservationId(Long reservationId) {
        TypedQuery<Payment> query = entityManager.createQuery(
                "SELECT p FROM Payment p WHERE p.reservation.id = :reservationId ORDER BY p.createdAt DESC",
                Payment.class);
        query.setParameter("reservationId", reservationId);
        return query.getResultList();
    }
}
