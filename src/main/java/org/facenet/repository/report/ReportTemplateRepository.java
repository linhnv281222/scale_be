package org.facenet.repository.report;

import org.facenet.entity.report.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    /**
     * Find by code
     */
    Optional<ReportTemplate> findByCode(String code);

    /**
     * Find default template by type
     */
    Optional<ReportTemplate> findByReportTypeAndIsDefaultTrue(ReportTemplate.ReportType reportType);

    /**
     * Find all active templates by type
     */
    List<ReportTemplate> findByReportTypeAndIsActiveTrue(ReportTemplate.ReportType reportType);

    /**
     * Find active templates
     */
    List<ReportTemplate> findByIsActiveTrueOrderByReportTypeAscNameAsc();

    /**
     * Find template with columns
     */
    @Query("SELECT DISTINCT t FROM ReportTemplate t LEFT JOIN FETCH t.columns WHERE t.id = :id")
    Optional<ReportTemplate> findByIdWithColumns(@Param("id") Long id);

    /**
     * Find by code with columns
     */
    @Query("SELECT DISTINCT t FROM ReportTemplate t LEFT JOIN FETCH t.columns WHERE t.code = :code")
    Optional<ReportTemplate> findByCodeWithColumns(@Param("code") String code);
}
