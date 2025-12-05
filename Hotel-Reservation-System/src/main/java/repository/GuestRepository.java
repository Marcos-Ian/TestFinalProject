package repository;

import model.Guest;

import java.util.List;
import java.util.Optional;

public interface GuestRepository {
    Guest save(Guest guest);
    Optional<Guest> findById(Long id);
    Optional<Guest> findByEmail(String email);
    List<Guest> findByName(String name);
    List<Guest> searchGuests(String name, String phone, String email, String address);
}
