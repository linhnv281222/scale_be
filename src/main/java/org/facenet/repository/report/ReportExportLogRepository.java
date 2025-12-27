package org.facenet.repository.report;

import org.facenet.entity.report.ReportExportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ReportExportLogRepository extends JpaRepository<ReportExportLog, Long> {

    /**
     * Find logs by user
     */
    Page<ReportExportLog> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    /**
     * Find logs in time range
     */
    @Query("SELECT l FROM ReportExportLog l WHERE l.createdAt BETWEEN :start AND :end ORDER BY l.createdAt DESC")
    List<ReportExportLog> findByCreatedAtBetween(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    /**
     * Find recent logs
     */
    List<ReportExportLog> findTop10ByOrderByCreatedAtDesc();

    /**
     * Count by status
     */
    long countByStatus(ReportExportLog.ExportStatus status);
}
