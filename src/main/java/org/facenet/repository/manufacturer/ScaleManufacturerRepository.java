package org.facenet.repository.manufacturer;

import org.facenet.entity.manufacturer.ScaleManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ScaleManufacturer entity
 */
@Repository
public interface ScaleManufacturerRepository extends JpaRepository<ScaleManufacturer, Long>, JpaSpecificationExecutor<ScaleManufacturer> {

    Optional<ScaleManufacturer> findByCode(String code);

    boolean existsByCode(String code);

    List<ScaleManufacturer> findByIsActive(Boolean isActive);

    /**
     * Find all active manufacturers
     */
    @Query("SELECT m FROM ScaleManufacturer m WHERE m.isActive = true ORDER BY m.name")
    List<ScaleManufacturer> findAllActive();

    /**
     * Search manufacturers by name or code
     */
    @Query("SELECT m FROM ScaleManufacturer m WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<ScaleManufacturer> searchByNameOrCode(String search);
}
