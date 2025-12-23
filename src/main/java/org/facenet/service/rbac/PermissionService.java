package org.facenet.service.rbac;

import org.facenet.dto.rbac.PermissionDto;
import org.facenet.entity.rbac.Permission;

import java.util.List;

/**
 * Service interface for Permission operations
 */
public interface PermissionService {

    /**
     * Get all permissions
     */
    List<PermissionDto> getAllPermissions();

    /**
     * Get permission by ID
     */
    PermissionDto getPermissionById(Integer id);

    /**
     * Get permission by code
     */
    Permission getPermissionByCode(String code);

    /**
     * Create a new permission
     */
    PermissionDto createPermission(String code, String description);
}
