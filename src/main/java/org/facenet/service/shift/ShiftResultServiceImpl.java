package org.facenet.service.shift;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.shift.ShiftResultResponseDto;
import org.facenet.dto.shift.ShiftResultUpdateRequestDto;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.shift.Shift;
import org.facenet.entity.shift.ShiftResult;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.shift.ShiftRepository;
import org.facenet.repository.shift.ShiftResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for shift result operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftResultServiceImpl implements ShiftResultService {

    private final ShiftResultRepository shiftResultRepository;
    private final ScaleRepository scaleRepository;
    private final ShiftRepository shiftRepository;

    @Override
    @Transactional(readOnly = true)
    public ShiftResultResponseDto getById(Long id) {
        log.info("[SHIFT-RESULT] Getting shift result by id: {}", id);
        ShiftResult shiftResult = shiftResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift result not found with id: " + id));
        return convertToDto(shiftResult);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftResultResponseDto getByScaleShiftAndDate(Long scaleId, Long shiftId, LocalDate shiftDate) {
        log.info("[SHIFT-RESULT] Getting shift result: scaleId={}, shiftId={}, date={}", 
                scaleId, shiftId, shiftDate);
        ShiftResult shiftResult = shiftResultRepository.findByScaleIdAndShiftIdAndShiftDate(scaleId, shiftId, shiftDate)
                .orElseThrow(() -> new RuntimeException("Shift result not found"));
        return convertToDto(shiftResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResultResponseDto> getByScaleId(Long scaleId) {
        log.info("[SHIFT-RESULT] Getting all shift results for scale: {}", scaleId);
        return shiftResultRepository.findByScaleIdOrderByShiftDateDesc(scaleId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResultResponseDto> getByDateRange(LocalDate fromDate, LocalDate toDate) {
        log.info("[SHIFT-RESULT] Getting shift results for date range: {} to {}", fromDate, toDate);
        return shiftResultRepository.findByDateRange(fromDate, toDate)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResultResponseDto> getByScaleIdAndDateRange(Long scaleId, LocalDate fromDate, LocalDate toDate) {
        log.info("[SHIFT-RESULT] Getting shift results for scale {} from {} to {}", scaleId, fromDate, toDate);
        return shiftResultRepository.findByScaleIdAndDateRange(scaleId, fromDate, toDate)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShiftResultResponseDto update(Long id, ShiftResultUpdateRequestDto request) {
        log.info("[SHIFT-RESULT] Updating shift result: {}", id);
        
        ShiftResult shiftResult = shiftResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift result not found with id: " + id));

        if (request.getStartValueData1() != null) {
            shiftResult.setStartValueData1(request.getStartValueData1());
        }

        if (request.getEndValueData1() != null) {
            shiftResult.setEndValueData1(request.getEndValueData1());
        }

        // Calculate deviation if both start and end values are available
        if (shiftResult.getStartValueData1() != null && shiftResult.getEndValueData1() != null) {
            BigDecimal deviation = calculateDeviation(
                    shiftResult.getStartValueData1(), 
                    shiftResult.getEndValueData1()
            );
            shiftResult.setDeviationData1(deviation);
        } else if (request.getDeviationData1() != null) {
            shiftResult.setDeviationData1(request.getDeviationData1());
        }

        if (request.getIsActive() != null) {
            shiftResult.setIsActive(request.getIsActive());
        }

        ShiftResult saved = shiftResultRepository.save(shiftResult);
        log.info("[SHIFT-RESULT] Updated shift result: {}", saved.getId());
        
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[SHIFT-RESULT] Deleting shift result: {}", id);
        ShiftResult shiftResult = shiftResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift result not found with id: " + id));
        shiftResult.setIsActive(false);
        shiftResultRepository.save(shiftResult);
        log.info("[SHIFT-RESULT] Soft deleted shift result: {}", id);
    }

    @Override
    @Transactional
    public ShiftResultResponseDto createOrUpdateStartValue(Long scaleId, Long shiftId, LocalDate shiftDate, String startValue) {
        log.info("[SHIFT-RESULT] Creating/updating start value: scaleId={}, shiftId={}, date={}, value={}", 
                scaleId, shiftId, shiftDate, startValue);

        // Check if shift result already exists
        ShiftResult shiftResult = shiftResultRepository
                .findByScaleIdAndShiftIdAndShiftDate(scaleId, shiftId, shiftDate)
                .orElse(null);

        if (shiftResult == null) {
            // Create new shift result
            Scale scale = scaleRepository.findById(scaleId)
                    .orElseThrow(() -> new RuntimeException("Scale not found with id: " + scaleId));
            Shift shift = shiftRepository.findById(shiftId)
                    .orElseThrow(() -> new RuntimeException("Shift not found with id: " + shiftId));

            shiftResult = ShiftResult.builder()
                    .scale(scale)
                    .shift(shift)
                    .shiftDate(shiftDate)
                    .startValueData1(startValue)
                    .build();
            
            log.info("[SHIFT-RESULT] Created new shift result for scale {} shift {} on {}", scaleId, shiftId, shiftDate);
        } else {
            // Update existing shift result
            shiftResult.setStartValueData1(startValue);
            
            // Recalculate deviation if end value exists
            if (shiftResult.getEndValueData1() != null) {
                BigDecimal deviation = calculateDeviation(startValue, shiftResult.getEndValueData1());
                shiftResult.setDeviationData1(deviation);
            }
            
            log.info("[SHIFT-RESULT] Updated start value for existing shift result: {}", shiftResult.getId());
        }

        ShiftResult saved = shiftResultRepository.save(shiftResult);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public ShiftResultResponseDto updateEndValue(Long scaleId, Long shiftId, LocalDate shiftDate, String endValue) {
        log.info("[SHIFT-RESULT] Updating end value: scaleId={}, shiftId={}, date={}, value={}", 
                scaleId, shiftId, shiftDate, endValue);

        ShiftResult shiftResult = shiftResultRepository
                .findByScaleIdAndShiftIdAndShiftDate(scaleId, shiftId, shiftDate)
                .orElseThrow(() -> new RuntimeException(
                        "Shift result not found for scale " + scaleId + ", shift " + shiftId + " on " + shiftDate));

        shiftResult.setEndValueData1(endValue);

        // Calculate deviation if start value exists
        if (shiftResult.getStartValueData1() != null) {
            BigDecimal deviation = calculateDeviation(shiftResult.getStartValueData1(), endValue);
            shiftResult.setDeviationData1(deviation);
            log.info("[SHIFT-RESULT] Calculated deviation: {} (start={}, end={})", 
                    deviation, shiftResult.getStartValueData1(), endValue);
        }

        ShiftResult saved = shiftResultRepository.save(shiftResult);
        log.info("[SHIFT-RESULT] Updated end value for shift result: {}", saved.getId());
        
        return convertToDto(saved);
    }

    /**
     * Calculate deviation (end - start)
     */
    private BigDecimal calculateDeviation(String startValue, String endValue) {
        try {
            BigDecimal start = new BigDecimal(startValue);
            BigDecimal end = new BigDecimal(endValue);
            return end.subtract(start);
        } catch (NumberFormatException e) {
            log.warn("[SHIFT-RESULT] Failed to calculate deviation: start={}, end={}", startValue, endValue);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Convert entity to DTO
     */
    private ShiftResultResponseDto convertToDto(ShiftResult entity) {
        return ShiftResultResponseDto.builder()
                .id(entity.getId())
                .scaleId(entity.getScale().getId())
                .scaleName(entity.getScale().getName())
                .shiftId(entity.getShift().getId())
                .shiftCode(entity.getShift().getCode())
                .shiftName(entity.getShift().getName())
                .shiftDate(entity.getShiftDate())
                .startValueData1(entity.getStartValueData1())
                .endValueData1(entity.getEndValueData1())
                .deviationData1(entity.getDeviationData1())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
