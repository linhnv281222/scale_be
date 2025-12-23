package org.facenet.service.rbac;

import org.facenet.dto.rbac.RoleDto;
import org.facenet.entity.rbac.Role;

import java.util.List;

/**
 * Service interface for Role operations
 */
public interface RoleService {

    /**
     * Get all roles
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
