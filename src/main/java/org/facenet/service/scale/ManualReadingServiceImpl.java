package org.facenet.service.scale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.scale.ManualReadingRequestDto;
import org.facenet.dto.scale.ManualReadingResponseDto;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.entity.shift.Shift;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.scale.WeighingLogRepository;
import org.facenet.repository.shift.ShiftRepository;
import org.facenet.service.shift.ShiftDetectionService;
import org.facenet.service.shift.ShiftResultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for manual reading operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManualReadingServiceImpl implements ManualReadingService {
    
    private final ScaleRepository scaleRepository;
    private final ScaleCurrentStateRepository currentStateRepository;
    private final WeighingLogRepository weighingLogRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftDetectionService shiftDetectionService;
    private final ShiftResultService shiftResultService;
    
    @Override
    @Transactional
    public ManualReadingResponseDto performManualReading(ManualReadingRequestDto request) {
        log.info("[MANUAL-READING] Starting manual reading: type={}, scaleIds={}, shiftId={}", 
                request.getReadingType(), request.getScaleIds(), request.getShiftId());
        
        OffsetDateTime triggeredAt = OffsetDateTime.now();
        
        // Determine which scales to read
        List<Scale> scalesToRead = resolveScalesToRead(request);
        
        // Get shift info if applicable
        Shift shift = null;
        if (request.getShiftId() != null) {
            shift = shiftRepository.findById(request.getShiftId()).orElse(null);
        } else if (request.getReadingType() != ManualReadingRequestDto.ReadingType.ON_DEMAND) {
            // Auto-detect current shift for SHIFT_START/SHIFT_END
            Optional<Shift> detectedShift = shiftDetectionService.detectCurrentShift(triggeredAt);
            shift = detectedShift.orElse(null);
        }
        
        // Perform readings
        List<ManualReadingResponseDto.ScaleReadingResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (Scale scale : scalesToRead) {
            try {
                ManualReadingResponseDto.DataValues dataValues = readScaleData(scale.getId());
                
                if (dataValues != null) {
                    // Save to weighing log
                    saveManualReading(scale, dataValues, shift, request);
                    
                    // Update shift result if applicable
                    updateShiftResult(scale, dataValues, shift, request);
                    
                    results.add(ManualReadingResponseDto.ScaleReadingResult.builder()
                            .scaleId(scale.getId())
                            .scaleName(scale.getName())
                            .success(true)
                            .message("Reading successful")
                            .dataValues(dataValues)
                            .build());
                    successCount++;
                } else {
                    results.add(ManualReadingResponseDto.ScaleReadingResult.builder()
                            .scaleId(scale.getId())
                            .scaleName(scale.getName())
                            .success(false)
                            .message("No current data available")
                            .build());
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("[MANUAL-READING] Failed to read scale {}: {}", scale.getId(), e.getMessage());
                results.add(ManualReadingResponseDto.ScaleReadingResult.builder()
                        .scaleId(scale.getId())
                        .scaleName(scale.getName())
                        .success(false)
                        .message("Error: " + e.getMessage())
                        .build());
                failedCount++;
            }
        }
        
        log.info("[MANUAL-READING] Completed: total={}, success={}, failed={}", 
                scalesToRead.size(), successCount, failedCount);
        
        return ManualReadingResponseDto.builder()
                .triggeredAt(triggeredAt)
                .readingType(request.getReadingType())
                .shiftId(shift != null ? shift.getId() : null)
                .shiftCode(shift != null ? shift.getCode() : null)
                .totalScales(scalesToRead.size())
                .successfulReadings(successCount)
                .failedReadings(failedCount)
                .results(results)
                .build();
    }
    
    @Override
    public ManualReadingResponseDto.DataValues readScaleData(Long scaleId) {
        log.debug("[MANUAL-READING] Reading current data from scale {}", scaleId);
        
        // Read from current state (latest data from auto reading)
        Optional<ScaleCurrentState> currentState = currentStateRepository.findById(scaleId);
        
        if (currentState.isEmpty()) {
            log.warn("[MANUAL-READING] No current state found for scale {}", scaleId);
            return null;
        }
        
        ScaleCurrentState state = currentState.get();
        
        return ManualReadingResponseDto.DataValues.builder()
                .data1(state.getData1())
                .data2(state.getData2())
                .data3(state.getData3())
                .data4(state.getData4())
                .data5(state.getData5())
                .status(state.getStatus())
                .build();
    }
    
    /**
     * Resolve which scales to read based on request
     */
    private List<Scale> resolveScalesToRead(ManualReadingRequestDto request) {
        if (request.getScaleIds() != null && !request.getScaleIds().isEmpty()) {
            // Read specific scales
            return scaleRepository.findAllById(request.getScaleIds());
        } else {
            // Read all active scales
            return scaleRepository.findAll().stream()
                    .filter(scale -> scale.getIsActive() != null && scale.getIsActive())
                    .toList();
        }
    }
    
    /**
     * Save manual reading to weighing log
     */
    private void saveManualReading(Scale scale, ManualReadingResponseDto.DataValues dataValues, 
                                   Shift shift, ManualReadingRequestDto request) {
        
        OffsetDateTime now = OffsetDateTime.now();
        
        // Create note to indicate manual reading
        String createdBy = String.format("manual_%s", request.getReadingType().name().toLowerCase());
        if (request.getNote() != null && !request.getNote().isBlank()) {
            createdBy += "_" + request.getNote().replaceAll("\\s+", "_");
        }
        
        WeighingLog weighingLog = WeighingLog.builder()
                .scaleId(scale.getId())
                .createdAt(now)
                .lastTime(now)
                .shift(shift)
                .data1(dataValues.getData1())
                .data2(dataValues.getData2())
                .data3(dataValues.getData3())
                .data4(dataValues.getData4())
                .data5(dataValues.getData5())
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();
        
        weighingLogRepository.save(weighingLog);
        
        log.info("[MANUAL-READING] Saved manual reading for scale {} with shift {} and type {}", 
                scale.getId(), shift != null ? shift.getCode() : "none", request.getReadingType());
    }
    
    /**
     * Update shift result when manual reading at shift start or end
     */
    private void updateShiftResult(Scale scale, ManualReadingResponseDto.DataValues dataValues,
                                   Shift shift, ManualReadingRequestDto request) {
        // Only update shift result for SHIFT_START and SHIFT_END
        if (shift == null || 
            (request.getReadingType() != ManualReadingRequestDto.ReadingType.SHIFT_START && 
             request.getReadingType() != ManualReadingRequestDto.ReadingType.SHIFT_END)) {
            return;
        }
        
        try {
            // Determine shift date based on shift timing and current time
            LocalDate shiftDate = determineShiftDate(shift, request.getReadingType());
            String data1Value = dataValues.getData1();
            
            if (request.getReadingType() == ManualReadingRequestDto.ReadingType.SHIFT_START) {
                // Create or update shift result with start value
                shiftResultService.createOrUpdateStartValue(scale.getId(), shift.getId(), shiftDate, data1Value);
                log.info("[MANUAL-READING] Updated shift result start value for scale {} shift {} on {}", 
                        scale.getId(), shift.getCode(), shiftDate);
            } else if (request.getReadingType() == ManualReadingRequestDto.ReadingType.SHIFT_END) {
                // Update shift result with end value
                shiftResultService.updateEndValue(scale.getId(), shift.getId(), shiftDate, data1Value);
                log.info("[MANUAL-READING] Updated shift result end value for scale {} shift {} on {}", 
                        scale.getId(), shift.getCode(), shiftDate);
            }
        } catch (Exception e) {
            // Don't fail the manual reading if shift result update fails
            log.warn("[MANUAL-READING] Failed to update shift result for scale {}: {}", 
                    scale.getId(), e.getMessage());
        }
    }
    
    /**
     * Determine shift date based on shift timing
     * For overnight shifts, if current time is before shift start time and it's SHIFT_END,
     * the shift date should be yesterday (because shift started yesterday)
     */
    private LocalDate determineShiftDate(Shift shift, ManualReadingRequestDto.ReadingType readingType) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // Check if this is an overnight shift (end time < start time)
        boolean isOvernightShift = shift.getEndTime().isBefore(shift.getStartTime());
        
        if (!isOvernightShift) {
            // Same-day shift: always use today
            return today;
        }
        
        // Overnight shift logic
        if (readingType == ManualReadingRequestDto.ReadingType.SHIFT_START) {
            // Shift start is always on the current date
            return today;
        } else {
            // SHIFT_END for overnight shift
            // If current time is before shift start time, it means we're in the "next day" part
            // So the shift actually started yesterday
            if (now.isBefore(shift.getStartTime())) {
                return today.minusDays(1);
            } else {
                // Current time is after shift start time, shift started today
                return today;
            }
        }
    }
}
