package org.facenet.service.rbac;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.rbac.PermissionDto;
import org.facenet.entity.rbac.Permission;
import org.facenet.mapper.RbacMapper;
import org.facenet.repository.rbac.PermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for Permission operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    public PageResponseDto<PermissionDto.Response> getAllPermissions(PageRequestDto pageRequest, Map<String, String> filters) {
        GenericSpecification<Permission> spec = new GenericSpecification<>();
        Specification<Permission> specification = spec.buildSpecification(filters);
        
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            Specification<Permission> searchSpec = spec.buildSearchSpecification(
                pageRequest.getSearch(), "code", "description"
            );
            specification = specification.and(searchSpec);
        }
        
        PageRequest springPageRequest = pageRequest.toPageRequest();
        Page<Permission> page = permissionRepository.findAll(specification, springPageRequest);
        Page<PermissionDto.Response> responsePage = page.map(RbacMapper::toResponseDto);
        
        return PageResponseDto.from(responsePage);
    }

    @Override
    public List<PermissionDto.Response> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(RbacMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDto.Response getPermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        return RbacMapper.toResponseDto(permission);
    }

    @Override
    @Transactional
    public PermissionDto.Response createPermission(PermissionDto.Request request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Permission", "code", request.getCode());
        }

        Permission permission = Permission.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .build();

        permission = permissionRepository.save(permission);
        return RbacMapper.toResponseDto(permission);
    }

    @Override
    @Transactional
    public PermissionDto.Response updatePermission(Integer id, PermissionDto.Request request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        // Check if code is being changed and if it conflicts
        if (!permission.getCode().equals(request.getCode()) && permissionRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Permission", "code", request.getCode());
        }

        permission.setCode(request.getCode());
        permission.setDescription(request.getDescription());

        permission = permissionRepository.save(permission);
        return RbacMapper.toResponseDto(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        // Soft delete: set is_active to false
        permission.setIsActive(false);
        permissionRepository.save(permission);
    }
}
