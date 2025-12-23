package org.facenet.mapper;

import org.facenet.dto.scale.ScaleDto;
import org.facenet.dto.scale.ScaleConfigDto;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.ScaleCurrentState;

/**
 * Mapper for Scale entities to DTOs
 */
public class ScaleMapper {

    public static ScaleDto.Response toResponseDto(Scale scale) {
        if (scale == null) return null;
        return ScaleDto.Response.builder()
                .id(scale.getId())
                .name(scale.getName())
                .locationId(scale.getLocation() != null ? scale.getLocation().getId() : null)
                .locationName(scale.getLocation() != null ? scale.getLocation().getName() : null)
                .model(scale.getModel())
                .isActive(scale.getIsActive())
                .scaleConfig(scale.getConfig() != null ? toScaleConfigResponseDto(scale.getConfig()) : null)
                .createdAt(scale.getCreatedAt())
                .createdBy(scale.getCreatedBy())
                .updatedAt(scale.getUpdatedAt())
                .updatedBy(scale.getUpdatedBy())
                .build();
    }

    private static ScaleConfigDto.Response toScaleConfigResponseDto(ScaleConfig config) {
        if (config == null) return null;
        return ScaleConfigDto.Response.builder()
                .scaleId(config.getScale() != null ? config.getScale().getId() : null)
                .protocol(config.getProtocol())
                .pollInterval(config.getPollInterval())
                .connParams(config.getConnParams())
                .data1(config.getData1())
                .data2(config.getData2())
                .data3(config.getData3())
                .data4(config.getData4())
                .data5(config.getData5())
                .build();
    }

    public static ScaleDto.WithState toWithStateDto(Scale scale) {
        if (scale == null) return null;
        return ScaleDto.WithState.builder()
                .id(scale.getId())
                .name(scale.getName())
                .model(scale.getModel())
                .isActive(scale.getIsActive())
                .currentState(toStateDto(scale.getCurrentState()))
                .build();
    }

    public static ScaleDto.StateDto toStateDto(ScaleCurrentState state) {
        if (state == null) return null;
        return ScaleDto.StateDto.builder()
                .data1(state.getData1())
                .data2(state.getData2())
                .data3(state.getData3())
                .data4(state.getData4())
                .data5(state.getData5())
                .status(state.getStatus())
                .lastTime(state.getLastTime())
                .build();
    }

    public static ScaleConfigDto.Response toConfigDto(ScaleConfig config) {
        if (config == null) return null;
        return ScaleConfigDto.Response.builder()
                .scaleId(config.getScaleId())
                .protocol(config.getProtocol())
                .pollInterval(config.getPollInterval())
                .connParams(config.getConnParams())
                .data1(config.getData1())
                .data2(config.getData2())
                .data3(config.getData3())
                .data4(config.getData4())
                .data5(config.getData5())
                .updatedAt(config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy())
                .build();
    }
}
