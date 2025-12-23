package org.facenet.service.rbac;

import org.facenet.dto.rbac.UserDto;

import java.util.List;

/**
 * Service interface for User operations
 */
public interface UserService {

    /**
     * Get all users
     */
    List<UserDto.Simple> getAllUsers();

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
