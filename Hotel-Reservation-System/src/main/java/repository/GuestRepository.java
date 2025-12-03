package repository;

import model.Guest;

import java.util.List;
import java.util.Optional;

public interface GuestRepository {
    Guest save(Guest guest);
    Optional<Guest> findById(Long id);
    List<Guest> findByName(String name);
}
