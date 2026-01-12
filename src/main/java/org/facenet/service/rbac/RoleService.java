package org.facenet.service.rbac;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.rbac.RoleDto;
import org.facenet.entity.rbac.Role;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Role operations
 */
public interface RoleService {

    /**
     * Get all roles with pagination and filters
     */
    PageResponseDto<RoleDto.Response> getAllRoles(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all roles (non-paginated)
     */
    List<RoleDto.Response> getAllRoles();

    /**
     * Get role by ID with permissions
     */
    RoleDto.Response getRoleById(Integer id);

    /**
     * Get role entity by ID
     */
    Role getRoleEntityById(Integer id);

    /**
     * Create a new role with permissions
     */
    RoleDto.Response createRole(RoleDto.Request request);

    /**
     * Update role
     */
    RoleDto.Response updateRole(Integer id, RoleDto.Request request);

    /**
     * Delete role
     */
    void deleteRole(Integer id);
}
