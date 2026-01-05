package org.facenet.mapper;

import org.facenet.dto.manufacturer.ScaleManufacturerDto;
import org.facenet.entity.manufacturer.ScaleManufacturer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for ScaleManufacturer entities to DTOs
 */
public class ScaleManufacturerMapper {

    public static ScaleManufacturerDto.Response toResponseDto(ScaleManufacturer manufacturer) {
        if (manufacturer == null) return null;
        return ScaleManufacturerDto.Response.builder()
                .id(manufacturer.getId())
                .code(manufacturer.getCode())
                .name(manufacturer.getName())
                .country(manufacturer.getCountry())
                .website(manufacturer.getWebsite())
                .phone(manufacturer.getPhone())
                .email(manufacturer.getEmail())
                .address(manufacturer.getAddress())
                .description(manufacturer.getDescription())
                .isActive(manufacturer.getIsActive())
                .createdAt(manufacturer.getCreatedAt())
                .createdBy(manufacturer.getCreatedBy())
                .updatedAt(manufacturer.getUpdatedAt())
                .updatedBy(manufacturer.getUpdatedBy())
                .build();
    }

    public static ScaleManufacturerDto.Simple toSimpleDto(ScaleManufacturer manufacturer) {
        if (manufacturer == null) return null;
        return ScaleManufacturerDto.Simple.builder()
                .id(manufacturer.getId())
                .code(manufacturer.getCode())
                .name(manufacturer.getName())
                .country(manufacturer.getCountry())
                .build();
    }

    public static List<ScaleManufacturerDto.Response> toResponseDtoList(List<ScaleManufacturer> manufacturers) {
        return manufacturers.stream()
                .map(ScaleManufacturerMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    public static List<ScaleManufacturerDto.Simple> toSimpleDtoList(List<ScaleManufacturer> manufacturers) {
        return manufacturers.stream()
                .map(ScaleManufacturerMapper::toSimpleDto)
                .collect(Collectors.toList());
    }
}
