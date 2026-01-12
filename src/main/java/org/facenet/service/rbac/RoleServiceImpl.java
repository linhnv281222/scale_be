package org.facenet.service.rbac;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.exception.ValidationException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.rbac.RoleDto;
import org.facenet.entity.rbac.Permission;
import org.facenet.entity.rbac.Role;
import org.facenet.mapper.RbacMapper;
import org.facenet.repository.rbac.PermissionRepository;
import org.facenet.repository.rbac.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for Role operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public PageResponseDto<RoleDto.Response> getAllRoles(PageRequestDto pageRequest, Map<String, String> filters) {
        GenericSpecification<Role> spec = new GenericSpecification<>();
        Specification<Role> specification = spec.buildSpecification(filters);
        
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            Specification<Role> searchSpec = spec.buildSearchSpecification(
                pageRequest.getSearch(), "name", "code"
            );
            specification = specification.and(searchSpec);
        }
        
        PageRequest springPageRequest = pageRequest.toPageRequest();
        Page<Role> page = roleRepository.findAll(specification, springPageRequest);
        Page<RoleDto.Response> responsePage = page.map(RbacMapper::toResponseDto);
        
        return PageResponseDto.from(responsePage);
    }

    @Override
    public List<RoleDto.Response> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(RbacMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDto.Response getRoleById(Integer id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return RbacMapper.toResponseDto(role);
    }

    @Override
    public Role getRoleEntityById(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
    }

    @Override
    @Transactional
    public RoleDto.Response createRole(RoleDto.Request request) {
        // Validate code uniqueness
        if (roleRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Role", "code", request.getCode());
        }

        // Validate permissions exist
        if (request.getPermissionIds() == null || request.getPermissionIds().isEmpty()) {
            throw new ValidationException("At least one permission is required");
        }

        Set<Permission> permissions = new HashSet<>();
        for (Integer permissionId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
            permissions.add(permission);
        }

        Role role = Role.builder()
                .name(request.getName())
                .code(request.getCode())
                .permissions(permissions)
                .build();

        role = roleRepository.save(role);
        return RbacMapper.toResponseDto(roleRepository.findByIdWithPermissions(role.getId()).get());
    }

    @Override
    @Transactional
    public RoleDto.Response updateRole(Integer id, RoleDto.Request request) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        // Check code uniqueness if changed
        if (!role.getCode().equals(request.getCode()) && 
            roleRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Role", "code", request.getCode());
        }

        // Update basic fields
        role.setName(request.getName());
        role.setCode(request.getCode());

        // Update permissions
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (Integer permissionId : request.getPermissionIds()) {
                Permission permission = permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
                permissions.add(permission);
            }
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        return RbacMapper.toResponseDto(role);
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        
        // Soft delete: set is_active to false
        role.setIsActive(false);
        roleRepository.save(role);
    }
}
