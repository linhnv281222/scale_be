package org.facenet.mapper;

import org.facenet.dto.location.LocationDto;
import org.facenet.entity.location.Location;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Location entities to DTOs
 */
public class LocationMapper {

    public static LocationDto.Response toResponseDto(Location location) {
        if (location == null) return null;
        return LocationDto.Response.builder()
                .id(location.getId())
                .code(location.getCode())
                .name(location.getName())
                .parentId(location.getParent() != null ? location.getParent().getId() : null)
                .children(location.getChildren() != null ?
                    location.getChildren().stream()
                        .map(LocationMapper::toResponseDto)
                        .collect(Collectors.toList()) : null)
                .createdAt(location.getCreatedAt())
                .createdBy(location.getCreatedBy())
                .updatedAt(location.getUpdatedAt())
                .updatedBy(location.getUpdatedBy())
                .build();
    }

    public static LocationDto.Simple toSimpleDto(Location location) {
        if (location == null) return null;
        return LocationDto.Simple.builder()
                .id(location.getId())
                .code(location.getCode())
                .name(location.getName())
                .build();
    }

    public static List<LocationDto.Response> toResponseDtoList(List<Location> locations) {
        return locations.stream()
                .map(LocationMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}