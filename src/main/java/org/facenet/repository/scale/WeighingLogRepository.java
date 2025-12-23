package org.facenet.repository.scale;

import org.facenet.entity.scale.WeighingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for WeighingLog entity (time-series data)
 */
@Repository
public interface WeighingLogRepository extends JpaRepository<WeighingLog, WeighingLog.WeighingLogId> {

    /**
     * Find logs by scale within time range
     */
    @Query("SELECT w FROM WeighingLog w WHERE w.scaleId = :scaleId " +
           "AND w.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY w.createdAt DESC")
    Page<WeighingLog> findByScaleIdAndTimeRange(
            @Param("scaleId") Long scaleId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            Pageable pageable
    );

    /**
     * Find latest log for a scale
     */
    @Query("SELECT w FROM WeighingLog w WHERE w.scaleId = :scaleId " +
           "ORDER BY w.createdAt DESC LIMIT 1")
    WeighingLog findLatestByScaleId(@Param("scaleId") Long scaleId);

    /**
     * Find all logs within time range (for reports)
     */
    @Query("SELECT w FROM WeighingLog w WHERE w.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY w.createdAt DESC")
    List<WeighingLog> findAllInTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}
