package org.facenet.service.scale;

import lombok.RequiredArgsConstructor;
import org.facenet.dto.scale.ScaleCurrentStateDto;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.ScaleRepository;
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

    /**
     * Get all scales' current states with config data names
     */
    public List<ScaleCurrentStateDto> getAllScalesWithConfig() {
        List<Scale> allScales = scaleRepository.findAll();
        
        return allScales.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ScaleCurrentStateDto convertToDto(Scale scale) {
        ScaleCurrentStateDto.ScaleCurrentStateDtoBuilder builder = ScaleCurrentStateDto.builder();
        
        builder.scaleId(scale.getId())
               .scaleName(scale.getName());

        // Get current state if exists
        Optional<ScaleCurrentState> currentState = scaleCurrentStateRepository.findById(scale.getId());
        
        if (currentState.isPresent()) {
            ScaleCurrentState state = currentState.get();
            builder.status(state.getStatus())
                   .lastTime(state.getLastTime());

            // Build data values map with names from config
            Map<String, ScaleCurrentStateDto.DataFieldValue> dataValues = new LinkedHashMap<>();
            
            if (scale.getConfig() != null) {
                ScaleConfig config = scale.getConfig();
                
                // data_1
                if (config.getData1() != null && !config.getData1().isEmpty()) {
                    dataValues.put("data_1", ScaleCurrentStateDto.DataFieldValue.builder()
                            .value(state.getData1())
                            .name((String) config.getData1().get("name"))
                            .isUsed((Boolean) config.getData1().getOrDefault("is_used", false))
                            .build());
                }
                
                // data_2
                if (config.getData2() != null && !config.getData2().isEmpty()) {
                    dataValues.put("data_2", ScaleCurrentStateDto.DataFieldValue.builder()
                            .value(state.getData2())
                            .name((String) config.getData2().get("name"))
                            .isUsed((Boolean) config.getData2().getOrDefault("is_used", false))
                            .build());
                }
                
                // data_3
                if (config.getData3() != null && !config.getData3().isEmpty()) {
                    dataValues.put("data_3", ScaleCurrentStateDto.DataFieldValue.builder()
                            .value(state.getData3())
                            .name((String) config.getData3().get("name"))
                            .isUsed((Boolean) config.getData3().getOrDefault("is_used", false))
                            .build());
                }
                
                // data_4
                if (config.getData4() != null && !config.getData4().isEmpty()) {
                    dataValues.put("data_4", ScaleCurrentStateDto.DataFieldValue.builder()
                            .value(state.getData4())
                            .name((String) config.getData4().get("name"))
                            .isUsed((Boolean) config.getData4().getOrDefault("is_used", false))
                            .build());
                }
                
                // data_5
                if (config.getData5() != null && !config.getData5().isEmpty()) {
                    dataValues.put("data_5", ScaleCurrentStateDto.DataFieldValue.builder()
                            .value(state.getData5())
                            .name((String) config.getData5().get("name"))
                            .isUsed((Boolean) config.getData5().getOrDefault("is_used", false))
                            .build());
                }
            }
            
            builder.dataValues(dataValues);
        } else {
            // No current state yet
            builder.dataValues(new LinkedHashMap<>());
        }

        return builder.build();
    }
}
