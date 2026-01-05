package org.facenet.repository.protocol;

import org.facenet.entity.protocol.Protocol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Protocol entity
 */
@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, Long> {

    Optional<Protocol> findByCode(String code);

    boolean existsByCode(String code);

    List<Protocol> findByIsActive(Boolean isActive);

    /**
     * Find all active protocols
     */
    @Query("SELECT p FROM Protocol p WHERE p.isActive = true ORDER BY p.name")
    List<Protocol> findAllActive();

    /**
     * Find protocols by connection type
     */
    List<Protocol> findByConnectionType(String connectionType);

    /**
     * Search protocols by name or code
     */
    @Query("SELECT p FROM Protocol p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Protocol> searchByNameOrCode(String search);
}
