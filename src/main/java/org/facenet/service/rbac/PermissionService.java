package org.facenet.service.rbac;

import org.facenet.dto.rbac.PermissionDto;

import java.util.List;

/**
 * Service interface for Permission operations
 */
public interface PermissionService {

    /**
     * Get all permissions
     */
    List<PermissionDto.Response> getAllPermissions();

    /**
     * Get permission by ID
     */
    PermissionDto.Response getPermissionById(Integer id);

    /**
     * Create a new permission
     */
    PermissionDto.Response createPermission(PermissionDto.Request request);

    /**
     * Update an existing permission
     */
    PermissionDto.Response updatePermission(Integer id, PermissionDto.Request request);

    /**
     * Delete a permission
     */
    void deletePermission(Integer id);
}
