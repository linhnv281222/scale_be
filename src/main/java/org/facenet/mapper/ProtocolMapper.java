package org.facenet.mapper;

import org.facenet.dto.protocol.ProtocolDto;
import org.facenet.entity.protocol.Protocol;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Protocol entities to DTOs
 */
public class ProtocolMapper {

    public static ProtocolDto.Response toResponseDto(Protocol protocol) {
        if (protocol == null) return null;
        return ProtocolDto.Response.builder()
                .id(protocol.getId())
                .code(protocol.getCode())
                .name(protocol.getName())
                .description(protocol.getDescription())
                .connectionType(protocol.getConnectionType())
                .defaultPort(protocol.getDefaultPort())
                .defaultBaudRate(protocol.getDefaultBaudRate())
                .isActive(protocol.getIsActive())
                .configTemplate(protocol.getConfigTemplate())
                .createdAt(protocol.getCreatedAt())
                .createdBy(protocol.getCreatedBy())
                .updatedAt(protocol.getUpdatedAt())
                .updatedBy(protocol.getUpdatedBy())
                .build();
    }

    public static ProtocolDto.Simple toSimpleDto(Protocol protocol) {
        if (protocol == null) return null;
        return ProtocolDto.Simple.builder()
                .id(protocol.getId())
                .code(protocol.getCode())
                .name(protocol.getName())
                .connectionType(protocol.getConnectionType())
                .build();
    }

    public static List<ProtocolDto.Response> toResponseDtoList(List<Protocol> protocols) {
        return protocols.stream()
                .map(ProtocolMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    public static List<ProtocolDto.Simple> toSimpleDtoList(List<Protocol> protocols) {
        return protocols.stream()
                .map(ProtocolMapper::toSimpleDto)
                .collect(Collectors.toList());
    }
}
