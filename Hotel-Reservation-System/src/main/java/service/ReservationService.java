package service;

import model.Guest;
import model.Reservation;
import repository.GuestRepository;
import repository.ReservationRepository;
import util.ValidationUtils;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Business layer gateway that validates reservations before persistence.
 */
public class ReservationService {
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(GuestRepository guestRepository, ReservationRepository reservationRepository) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
    }

    public Reservation createReservation(Guest guest, LocalDate checkIn, LocalDate checkOut) {
        ValidationUtils.require(checkIn != null && checkOut != null && !checkOut.isBefore(checkIn),
                "Check-out date must be after check-in date.");
        Guest persisted = guestRepository.save(guest);
        Reservation reservation = new Reservation();
        reservation.setGuest(persisted);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setStatus("BOOKED");
        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }
}
