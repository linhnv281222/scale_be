package org.facenet.service.rbac;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.rbac.UserDto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for User operations
 */
public interface UserService {

    /**
     * Get all users with pagination and filters
     */
    PageResponseDto<UserDto.Response> getAllUsers(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all users with roles and permissions (non-paginated)
     */
    List<UserDto.Response> getAllUsers();

    /**
     * Get user by ID with roles and permissions (3 levels deep)
     */
    UserDto.Response getUserById(Long id);

    /**
     * Create a new user with roles
     */
    UserDto.Response createUser(UserDto.CreateRequest request);

    /**
     * Update user
     */
    UserDto.Response updateUser(Long id, UserDto.UpdateRequest request);

    /**
     * Update user roles
     */
    UserDto.Response updateUserRoles(Long id, List<Integer> roleIds);

    /**
     * Delete user
     */
    void deleteUser(Long id);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
}
