package org.facenet.service.shift;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.shift.Shift;
import org.facenet.repository.shift.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for automatically detecting the current shift based on time
 * Handles shift detection logic for scale data collection
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftDetectionService {

    private final ShiftRepository shiftRepository;

    /**
     * Detect current active shift based on given timestamp
     * 
     * @param timestamp The timestamp to check
     * @return Optional containing the matching shift, or empty if no shift matches
     */
    public Optional<Shift> detectCurrentShift(OffsetDateTime timestamp) {
        if (timestamp == null) {
            log.debug("[SHIFT-DETECT] Timestamp is null, cannot detect shift");
            return Optional.empty();
        }
        
        LocalTime timeToCheck = timestamp.toLocalTime();
        
        // Get all active shifts
        List<Shift> activeShifts = shiftRepository.findAll().stream()
                .filter(shift -> shift.getIsActive() != null && shift.getIsActive())
                .filter(shift -> shift.getStartTime() != null && shift.getEndTime() != null)
                .toList();

        if (activeShifts.isEmpty()) {
            log.debug("[SHIFT-DETECT] No active shifts found");
            return Optional.empty();
        }

        // Find matching shift
        for (Shift shift : activeShifts) {
            if (isTimeInShift(timeToCheck, shift)) {
                log.debug("[SHIFT-DETECT] Time {} matches shift {} ({}->{})", 
                    timeToCheck, shift.getCode(), shift.getStartTime(), shift.getEndTime());
                return Optional.of(shift);
            }
        }

        log.debug("[SHIFT-DETECT] Time {} does not match any active shift", timeToCheck);
        return Optional.empty();
    }

    /**
     * Check if a given time falls within a shift's time range
     * Handles both same-day shifts (e.g., 08:00-16:00) and overnight shifts (e.g., 22:00-06:00)
     * 
     * @param time The time to check
     * @param shift The shift with start and end times
     * @return true if time is within shift range
     */
    private boolean isTimeInShift(LocalTime time, Shift shift) {
        LocalTime startTime = shift.getStartTime();
        LocalTime endTime = shift.getEndTime();

        if (startTime == null || endTime == null) {
            log.warn("Shift {} has null start or end time", shift.getCode());
            return false;
        }

        // Case 1: Same-day shift (start < end)
        // Example: 08:00 - 16:00
        if (startTime.isBefore(endTime)) {
            return !time.isBefore(startTime) && time.isBefore(endTime);
        }
        
        // Case 2: Overnight shift (start >= end)
        // Example: 22:00 - 06:00 (crosses midnight)
        // Time is in shift if: time >= 22:00 OR time < 06:00
        return !time.isBefore(startTime) || time.isBefore(endTime);
    }

    /**
     * Detect current shift using current system time
     * 
     * @return Optional containing the current shift, or empty if no shift matches
     */
    public Optional<Shift> detectCurrentShift() {
        return detectCurrentShift(OffsetDateTime.now());
    }
}
