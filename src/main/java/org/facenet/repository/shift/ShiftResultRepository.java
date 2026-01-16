package org.facenet.repository.shift;

import org.facenet.entity.shift.ShiftResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ShiftResult entity
 */
@Repository
public interface ShiftResultRepository extends JpaRepository<ShiftResult, Long>, JpaSpecificationExecutor<ShiftResult> {

    /**
     * Find shift result by scale, shift, and date
     */
    Optional<ShiftResult> findByScaleIdAndShiftIdAndShiftDate(Long scaleId, Long shiftId, LocalDate shiftDate);

    /**
     * Find all shift results for a specific scale
     */
    List<ShiftResult> findByScaleIdOrderByShiftDateDesc(Long scaleId);

    /**
     * Find all shift results for a specific shift
     */
    List<ShiftResult> findByShiftIdOrderByShiftDateDesc(Long shiftId);

    /**
     * Find all shift results for a specific date
     */
    List<ShiftResult> findByShiftDateOrderByScaleIdAsc(LocalDate shiftDate);

    /**
     * Find all shift results for a scale within date range
     */
    @Query("SELECT sr FROM ShiftResult sr " +
           "WHERE sr.scale.id = :scaleId " +
           "AND sr.shiftDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY sr.shiftDate DESC, sr.shift.startTime ASC")
    List<ShiftResult> findByScaleIdAndDateRange(
        @Param("scaleId") Long scaleId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    /**
     * Find all shift results within date range
     */
    @Query("SELECT sr FROM ShiftResult sr " +
           "WHERE sr.shiftDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY sr.shiftDate DESC, sr.scale.id ASC, sr.shift.startTime ASC")
    List<ShiftResult> findByDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    /**
     * Check if shift result exists
     */
    boolean existsByScaleIdAndShiftIdAndShiftDate(Long scaleId, Long shiftId, LocalDate shiftDate);
}
