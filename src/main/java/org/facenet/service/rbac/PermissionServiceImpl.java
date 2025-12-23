package org.facenet.service.rbac;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.dto.rbac.PermissionDto;
import org.facenet.entity.rbac.Permission;
import org.facenet.mapper.RbacMapper;
import org.facenet.repository.rbac.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(RbacMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDto getPermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        return RbacMapper.toDto(permission);
    }

    @Override
    public Permission getPermissionByCode(String code) {
        return permissionRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "code", code));
    }

    @Override
    @Transactional
    public PermissionDto createPermission(String code, String description) {
        if (permissionRepository.existsByCode(code)) {
            throw new AlreadyExistsException("Permission", "code", code);
        }

        Permission permission = Permission.builder()
                .code(code)
                .description(description)
                .build();

        permission = permissionRepository.save(permission);
        return RbacMapper.toDto(permission);
    }
}
