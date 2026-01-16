package org.facenet.service.shift;

import org.facenet.dto.shift.ShiftResultResponseDto;
import org.facenet.dto.shift.ShiftResultUpdateRequestDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for shift result operations
 */
public interface ShiftResultService {

    /**
     * Get shift result by ID
     */
    ShiftResultResponseDto getById(Long id);

    /**
     * Get shift result by scale, shift, and date
     */
    ShiftResultResponseDto getByScaleShiftAndDate(Long scaleId, Long shiftId, LocalDate shiftDate);

    /**
     * Get all shift results for a scale
     */
    List<ShiftResultResponseDto> getByScaleId(Long scaleId);

    /**
     * Get all shift results within date range
     */
    List<ShiftResultResponseDto> getByDateRange(LocalDate fromDate, LocalDate toDate);

    /**
     * Get all shift results for a scale within date range
     */
    List<ShiftResultResponseDto> getByScaleIdAndDateRange(Long scaleId, LocalDate fromDate, LocalDate toDate);

    /**
     * Update shift result
     */
    ShiftResultResponseDto update(Long id, ShiftResultUpdateRequestDto request);

    /**
     * Delete shift result
     */
    void delete(Long id);

    /**
     * Create or update shift result with start value (called at shift start)
     */
    ShiftResultResponseDto createOrUpdateStartValue(Long scaleId, Long shiftId, LocalDate shiftDate, String startValue);

    /**
     * Update shift result with end value (called at shift end)
     */
    ShiftResultResponseDto updateEndValue(Long scaleId, Long shiftId, LocalDate shiftDate, String endValue);
}
