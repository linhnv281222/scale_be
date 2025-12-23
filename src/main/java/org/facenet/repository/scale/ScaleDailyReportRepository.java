package org.facenet.repository.scale;

import org.facenet.entity.scale.ScaleDailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ScaleDailyReport entity
 */
@Repository
public interface ScaleDailyReportRepository extends JpaRepository<ScaleDailyReport, ScaleDailyReport.ScaleDailyReportId> {

    /**
     * Find reports for a scale within date range
     */
    @Query("SELECT r FROM ScaleDailyReport r WHERE r.scaleId = :scaleId " +
           "AND r.date BETWEEN :startDate AND :endDate " +
           "ORDER BY r.date DESC")
    List<ScaleDailyReport> findByScaleIdAndDateRange(
            @Param("scaleId") Long scaleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find report for a specific scale on a specific date
     */
    Optional<ScaleDailyReport> findByDateAndScaleId(LocalDate date, Long scaleId);

    /**
     * Find all reports for a specific date
     */
    List<ScaleDailyReport> findByDate(LocalDate date);
}
