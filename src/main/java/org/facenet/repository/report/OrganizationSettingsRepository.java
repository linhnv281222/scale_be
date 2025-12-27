package org.facenet.repository.report;

import org.facenet.entity.report.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, Long> {

    /**
     * Find default organization settings
     */
    Optional<OrganizationSettings> findFirstByIsDefaultTrue();

    /**
     * Find active default organization
     */
    @Query("SELECT o FROM OrganizationSettings o WHERE o.isActive = true AND o.isDefault = true")
    Optional<OrganizationSettings> findActiveDefault();
}
