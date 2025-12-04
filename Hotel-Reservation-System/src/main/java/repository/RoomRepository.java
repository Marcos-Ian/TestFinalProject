// ============================================================================
// RoomRepository.java - Repository interface for Room operations
// ============================================================================
package repository;

import model.RoomType;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Room/RoomType persistence operations.
 */
public interface RoomRepository {

    /**
     * Save or update a room type
     */
    RoomType save(RoomType roomType);

    /**
     * Find room type by ID
     */
    Optional<RoomType> findById(Long id);

    /**
     * Find room type by type enum
     */
    Optional<RoomType> findByType(RoomType.Type type);

    /**
     * Find all room types
     */
    List<RoomType> findAll();

    /**
     * Delete a room type
     */
    void delete(Long id);

    /**
     * Count total rooms of a specific type
     */
    int countByType(RoomType.Type type);
}
