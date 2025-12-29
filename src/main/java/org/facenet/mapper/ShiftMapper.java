package org.facenet.mapper;

import org.facenet.dto.shift.ShiftDto;
import org.facenet.entity.shift.Shift;

import java.util.List;

public class ShiftMapper {

    private ShiftMapper() {
    }

    public static ShiftDto.Response toResponseDto(Shift shift) {
        if (shift == null) {
            return null;
        }

        return ShiftDto.Response.builder()
                .id(shift.getId())
                .code(shift.getCode())
                .name(shift.getName())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .isActive(shift.getIsActive())
                .createdAt(shift.getCreatedAt())
                .createdBy(shift.getCreatedBy())
                .updatedAt(shift.getUpdatedAt())
                .updatedBy(shift.getUpdatedBy())
                .build();
    }

    public static List<ShiftDto.Response> toResponseDtoList(List<Shift> shifts) {
        return shifts.stream().map(ShiftMapper::toResponseDto).toList();
    }

    public static Shift toEntity(ShiftDto.Request request) {
        if (request == null) {
            return null;
        }

        return Shift.builder()
                .code(request.getCode())
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }
}
