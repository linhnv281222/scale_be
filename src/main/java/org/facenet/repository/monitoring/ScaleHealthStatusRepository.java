package org.facenet.repository.monitoring;

import org.facenet.entity.monitoring.ScaleHealthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScaleHealthStatusRepository extends JpaRepository<ScaleHealthStatus, Long> {

    /**
     * Find active issues for a scale
     */
    List<ScaleHealthStatus> findByScaleIdAndIsActiveIssueTrue(Long scaleId);

    /**
     * Find all active issues
     */
    List<ScaleHealthStatus> findByIsActiveIssueTrueOrderByDetectedAtDesc();

    /**
     * Find by health status
     */
    List<ScaleHealthStatus> findByHealthStatusAndIsActiveIssueTrue(ScaleHealthStatus.HealthStatus healthStatus);

    /**
     * Find issues detected in time range
     */
    List<ScaleHealthStatus> findByDetectedAtBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * Find the latest active issue for a scale
     */
    @Query("SELECT h FROM ScaleHealthStatus h WHERE h.scale.id = :scaleId " +
           "AND h.isActiveIssue = true ORDER BY h.detectedAt DESC")
    Optional<ScaleHealthStatus> findLatestActiveIssueByScaleId(@Param("scaleId") Long scaleId);

    /**
     * Count active issues by health status
     */
    @Query("SELECT h.healthStatus, COUNT(h) FROM ScaleHealthStatus h " +
           "WHERE h.isActiveIssue = true GROUP BY h.healthStatus")
    List<Object[]> countActiveIssuesByHealthStatus();

    /**
     * Find scales with issues longer than duration
     */
    @Query("SELECT h FROM ScaleHealthStatus h WHERE h.isActiveIssue = true " +
           "AND h.detectedAt < :thresholdTime ORDER BY h.detectedAt ASC")
    List<ScaleHealthStatus> findLongRunningIssues(@Param("thresholdTime") OffsetDateTime thresholdTime);

    /**
     * Count active issues
     */
    long countByIsActiveIssueTrue();

    /**
     * Find by scale IDs and active
     */
    List<ScaleHealthStatus> findByScaleIdInAndIsActiveIssueTrue(List<Long> scaleIds);
}
