package org.facenet.repository.location;

import org.facenet.entity.location.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Location entity
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByCode(String code);

    boolean existsByCode(String code);

    List<Location> findByParentId(Long parentId);

    /**
     * Find all root locations (no parent)
     */
    List<Location> findByParentIsNull();

    /**
     * Find location with children eagerly loaded
     */
    @Query("SELECT l FROM Location l LEFT JOIN FETCH l.children WHERE l.id = :id")
    Optional<Location> findByIdWithChildren(Long id);
}
