package org.facenet.repository.report;

import org.facenet.entity.report.ReportDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ReportDefinition
 */
@Repository
public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {

    /**
     * Find report by code
     */
    Optional<ReportDefinition> findByReportCode(String reportCode);

    /**
     * Find all active reports
     */
    List<ReportDefinition> findByStatusOrderByDisplayOrderAsc(ReportDefinition.ReportStatus status);

    /**
     * Find reports by category
     */
    List<ReportDefinition> findByCategoryAndStatusOrderByDisplayOrderAsc(
            String category, 
            ReportDefinition.ReportStatus status
    );

    /**
     * Check if report code exists
     */
    boolean existsByReportCode(String reportCode);

    /**
     * Find executable reports (ACTIVE status)
     */
    @Query("SELECT rd FROM ReportDefinition rd WHERE rd.status = 'ACTIVE' ORDER BY rd.displayOrder ASC, rd.reportName ASC")
    List<ReportDefinition> findExecutableReports();

    /**
     * Find reports supporting specific format
     * Note: Using native query for PostgreSQL array containment check
     */
    @Query(value = "SELECT * FROM report_definitions rd WHERE rd.status = 'ACTIVE' AND :format = ANY(rd.supported_formats)", nativeQuery = true)
    List<ReportDefinition> findByFormat(@Param("format") String format);
}
