package org.facenet.service.rbac;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.rbac.PermissionDto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Permission operations
 */
public interface PermissionService {

    /**
     * Get all permissions with pagination and filters
     */
    PageResponseDto<PermissionDto.Response> getAllPermissions(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all permissions (non-paginated)
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
