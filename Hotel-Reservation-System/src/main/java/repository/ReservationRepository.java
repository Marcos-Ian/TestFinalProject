package repository;

import model.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findByGuestName(String name);
    List<Reservation> findByDateRange(LocalDate from, LocalDate to);
}
