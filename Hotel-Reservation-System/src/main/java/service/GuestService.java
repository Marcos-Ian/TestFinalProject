package service;

import model.Guest;
import model.Reservation;
import repository.GuestRepository;
import repository.ReservationRepository;

import java.util.List;
import java.util.UUID;

/**
 * Provides profile-level operations for guests.
 */
public class GuestService {
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    public GuestService(GuestRepository guestRepository, ReservationRepository reservationRepository) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Guest> searchGuests(String name, String phone, String email, String street, String city, String province, String postal) {
        return guestRepository.searchGuests(name, phone, email, street, city, province, postal);
    }

    public Guest enrollInLoyalty(Guest guest) {
        if (guest == null) {
            throw new IllegalArgumentException("Guest is required for loyalty enrollment");
        }
        if (guest.getLoyaltyNumber() != null && !guest.getLoyaltyNumber().isBlank()) {
            return guest;
        }
        String loyaltyNumber = "LOY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        guest.setLoyaltyNumber(loyaltyNumber);
        return guestRepository.save(guest);
    }

    public List<Reservation> findReservationsForGuest(Guest guest) {
        return reservationRepository.findByGuest(guest);
    }
}
