package org.facenet.repository.scale;

import org.facenet.entity.scale.Scale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Scale entity
 */
@Repository
public interface ScaleRepository extends JpaRepository<Scale, Long>, JpaSpecificationExecutor<Scale> {

    List<Scale> findByLocationId(Long locationId);

    List<Scale> findByLocationIdIn(List<Long> locationIds);

    List<Scale> findByIsActive(Boolean isActive);

    List<Scale> findByManufacturerId(Long manufacturerId);

    List<Scale> findByDirection(org.facenet.entity.scale.ScaleDirection direction);

    long countByIsActive(Boolean isActive);

    /**
     * Find scale with config and current state
     */
    @Query("SELECT s FROM Scale s " +
           "LEFT JOIN FETCH s.config " +
           "LEFT JOIN FETCH s.currentState " +
           "WHERE s.id = :id")
    Optional<Scale> findByIdWithDetails(@Param("id") Long id);

    /**
     * Find all active scales with configs
     */
    @Query("SELECT s FROM Scale s " +
           "LEFT JOIN FETCH s.config " +
           "WHERE s.isActive = true")
    List<Scale> findAllActiveWithConfig();
}
