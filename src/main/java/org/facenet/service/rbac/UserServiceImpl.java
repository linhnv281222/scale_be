package org.facenet.service.rbac;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.exception.ValidationException;
import org.facenet.dto.rbac.UserDto;
import org.facenet.entity.rbac.Role;
import org.facenet.entity.rbac.User;
import org.facenet.mapper.RbacMapper;
import org.facenet.repository.rbac.RoleRepository;
import org.facenet.repository.rbac.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for User operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserDto.Response> getAllUsers() {
        return userRepository.findAllWithRolesAndPermissions()
                .stream()
                .map(RbacMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto.Response getUserById(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return RbacMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    public UserDto.Response createUser(UserDto.CreateRequest request) {
        // Validate username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AlreadyExistsException(
                    String.format("Username '%s' đã được sử dụng.", request.getUsername()));
        }

        // Validate roles
        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new ValidationException("At least one role is required");
        }

        Set<Role> roles = new HashSet<>();
        for (Integer roleId : request.getRoleIds()) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
            roles.add(role);
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .status(request.getStatus() != null ? request.getStatus() : (short) 1)
                .roles(roles)
                .build();

        user = userRepository.save(user);
        
        // Reload with full data
        return getUserById(user.getId());
    }

    @Override
    @Transactional
    public UserDto.Response updateUser(Long id, UserDto.UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Update basic fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        // Update roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (Integer roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        return getUserById(user.getId());
    }

    @Override
    @Transactional
    public UserDto.Response updateUserRoles(Long id, List<Integer> roleIds) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (roleIds == null || roleIds.isEmpty()) {
            throw new ValidationException("At least one role is required");
        }

        Set<Role> roles = new HashSet<>();
        for (Integer roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
            roles.add(role);
        }

        user.setRoles(roles);
        userRepository.save(user);
        
        return getUserById(id);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
