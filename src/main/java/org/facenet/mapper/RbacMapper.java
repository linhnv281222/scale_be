package org.facenet.mapper;

import org.facenet.dto.rbac.PermissionDto;
import org.facenet.dto.rbac.RoleDto;
import org.facenet.dto.rbac.UserDto;
import org.facenet.entity.rbac.Permission;
import org.facenet.entity.rbac.Role;
import org.facenet.entity.rbac.User;

import java.util.stream.Collectors;

/**
 * Mapper for RBAC entities to DTOs
 */
public class RbacMapper {

    public static PermissionDto.Response toDto(Permission permission) {
        if (permission == null) return null;
        return PermissionDto.Response.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .createdBy(permission.getCreatedBy())
                .build();
    }

    public static PermissionDto.Response toResponseDto(Permission permission) {
        if (permission == null) return null;
        return PermissionDto.Response.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .createdBy(permission.getCreatedBy())
                .build();
    }

    public static RoleDto.Simple toSimpleDto(Role role) {
        if (role == null) return null;
        return RoleDto.Simple.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .build();
    }

    public static RoleDto.Response toResponseDto(Role role) {
        if (role == null) return null;
        return RoleDto.Response.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .permissions(role.getPermissions().stream()
                        .map(RbacMapper::toDto)
                        .collect(Collectors.toList()))
                .createdAt(role.getCreatedAt())
                .createdBy(role.getCreatedBy())
                .updatedAt(role.getUpdatedAt())
                .updatedBy(role.getUpdatedBy())
                .build();
    }

    public static UserDto.Simple toSimpleDto(User user) {
        if (user == null) return null;
        return UserDto.Simple.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .build();
    }

    public static UserDto.Response toResponseDto(User user) {
        if (user == null) return null;
        return UserDto.Response.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(user.getRoles().stream()
                        .map(role -> UserDto.RoleWithPermissions.builder()
                                .id(role.getId())
                                .name(role.getName())
                                .code(role.getCode())
                                .permissions(role.getPermissions().stream()
                                        .map(RbacMapper::toDto)
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .updatedAt(user.getUpdatedAt())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}
