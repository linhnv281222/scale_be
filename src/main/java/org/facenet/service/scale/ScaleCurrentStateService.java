package org.facenet.service.scale;

import lombok.RequiredArgsConstructor;
import org.facenet.dto.scale.ScaleCurrentStateDto;
import org.facenet.dto.scale.ScaleShiftDto;
import org.facenet.entity.shift.Shift;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.shift.ShiftRepository;
import org.facenet.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for scale current state
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScaleCurrentStateService {

    private final ScaleCurrentStateRepository scaleCurrentStateRepository;
    private final ScaleRepository scaleRepository;
    private final ShiftRepository shiftRepository;

    /**
     * Get all scales' current states with config data names
     */
    public List<ScaleCurrentStateDto> getAllScalesWithConfig() {
        return getScalesWithConfig(null, null, null);
    }

    /**
     * Get scales' current states with optional filters.
     *
     * @param scaleId   optional single scale id filter
     * @param scaleIds  optional multiple scale ids filter (takes precedence over scaleId)
     * @param status    optional scale status filter (matches scale_current_states.status, case-insensitive, trimmed)
     */
    public List<ScaleCurrentStateDto> getScalesWithConfig(Long scaleId, List<Long> scaleIds, String status) {
        List<Scale> scales;

        if (scaleIds != null && !scaleIds.isEmpty()) {
            scales = scaleRepository.findAllById(scaleIds);
        } else if (scaleId != null) {
            scales = scaleRepository.findById(scaleId)
                    .map(List::of)
                    .orElseGet(List::of);
        } else {
            scales = scaleRepository.findAll();
        }

        final String normalizedStatus = normalizeFilterValue(status);

        return scales.stream()
            .map(scale -> convertToDto(scale, normalizedStatus))
            .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ScaleCurrentStateDto convertToDto(Scale scale) {
        return convertToDto(scale, null);
    }

    private ScaleCurrentStateDto convertToDto(Scale scale, String normalizedStatus) {
        ScaleCurrentStateDto.ScaleCurrentStateDtoBuilder builder = ScaleCurrentStateDto.builder();
        
        builder.scaleId(scale.getId())
               .scaleName(scale.getName());

        // Get current state if exists
        Optional<ScaleCurrentState> currentState = scaleCurrentStateRepository.findById(scale.getId());
        
        if (currentState.isPresent()) {
            ScaleCurrentState state = currentState.get();

            if (normalizedStatus != null && !normalizedStatus.equalsIgnoreCase(normalizeFilterValue(state.getStatus()))) {
                return null;
            }

            builder.status(state.getStatus())
                   .lastTime(state.getLastTime());

            if (state.getShift() != null) {
                builder.shiftId(state.getShift().getId());
            }

            // Build data values map with names from config
            Map<String, ScaleCurrentStateDto.DataFieldValue> dataValues = new LinkedHashMap<>();
            
            if (scale.getConfig() != null) {
                ScaleConfig config = scale.getConfig();

                maybePutDataValue(dataValues, "data_1", state.getData1(), config.getData1());
                maybePutDataValue(dataValues, "data_2", state.getData2(), config.getData2());
                maybePutDataValue(dataValues, "data_3", state.getData3(), config.getData3());
                maybePutDataValue(dataValues, "data_4", state.getData4(), config.getData4());
                maybePutDataValue(dataValues, "data_5", state.getData5(), config.getData5());
            }
            
            builder.dataValues(dataValues);
        } else {
            // No current state yet
            if (normalizedStatus != null) {
                return null;
            }
            builder.dataValues(new LinkedHashMap<>());
        }

        return builder.build();
    }

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void maybePutDataValue(
            Map<String, ScaleCurrentStateDto.DataFieldValue> dataValues,
            String key,
            String value,
            Map<String, Object> config
    ) {
        if (config == null || config.isEmpty()) {
            return;
        }

        String configuredName = (String) config.get("name");

        dataValues.put(key, ScaleCurrentStateDto.DataFieldValue.builder()
                .value(value)
                .name(configuredName)
                .isUsed((Boolean) config.getOrDefault("is_used", false))
                .build());
    }

    @Transactional
    public void setScaleCurrentShift(Long scaleId, ScaleShiftDto.Request request) {
        Scale scale = scaleRepository.findById(scaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Scale", "id", scaleId));

        Shift shift = null;
        if (request != null && request.getShiftId() != null) {
            shift = shiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shift", "id", request.getShiftId()));
        }

        ScaleCurrentState currentState = scaleCurrentStateRepository.findById(scaleId)
                .orElseGet(() -> {
                    ScaleCurrentState newState = new ScaleCurrentState();
                    newState.setScale(scale);
                    // lastTime is required by schema; set to now for brand-new state row
                    newState.setLastTime(java.time.OffsetDateTime.now());
                    return newState;
                });

        currentState.setShift(shift);
        scaleCurrentStateRepository.save(currentState);
    }
}
