package org.facenet.repository.report;

import org.facenet.entity.report.ReportExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for ReportExecutionHistory
 */
@Repository
public interface ReportExecutionHistoryRepository extends JpaRepository<ReportExecutionHistory, Long> {

    /**
     * Find executions by report code
     */
    Page<ReportExecutionHistory> findByReportCodeOrderByExecutionStartTimeDesc(
            String reportCode, 
            Pageable pageable
    );

    /**
     * Find executions by user
     */
    Page<ReportExecutionHistory> findByExecutedByOrderByExecutionStartTimeDesc(
            String executedBy, 
            Pageable pageable
    );

    /**
     * Find executions by status
     */
    List<ReportExecutionHistory> findByStatusOrderByExecutionStartTimeDesc(
            ReportExecutionHistory.ExecutionStatus status
    );

    /**
     * Find recent executions
     */
    @Query("SELECT reh FROM ReportExecutionHistory reh ORDER BY reh.executionStartTime DESC")
    Page<ReportExecutionHistory> findRecentExecutions(Pageable pageable);

    /**
     * Find executions in time range
     */
    @Query("SELECT reh FROM ReportExecutionHistory reh WHERE reh.executionStartTime BETWEEN :start AND :end ORDER BY reh.executionStartTime DESC")
    List<ReportExecutionHistory> findByExecutionTimeRange(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    /**
     * Count executions by status
     */
    long countByStatus(ReportExecutionHistory.ExecutionStatus status);

    /**
     * Count executions by report code
     */
    long countByReportCode(String reportCode);

    /**
     * Find failed executions for monitoring
     */
    @Query("SELECT reh FROM ReportExecutionHistory reh WHERE reh.status = 'FAILED' AND reh.executionStartTime > :since ORDER BY reh.executionStartTime DESC")
    List<ReportExecutionHistory> findRecentFailures(@Param("since") OffsetDateTime since);

    /**
     * Calculate average execution time for a report
     */
    @Query("SELECT AVG(reh.executionTimeMs) FROM ReportExecutionHistory reh WHERE reh.reportCode = :reportCode AND reh.status = 'SUCCESS'")
    Double calculateAverageExecutionTime(@Param("reportCode") String reportCode);
}
